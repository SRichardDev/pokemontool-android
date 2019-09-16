package io.stanc.pogoradar.screen

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.stanc.pogoradar.MapFilterSettings
import io.stanc.pogoradar.Popup
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentMapInteractionBinding
import io.stanc.pogoradar.firebase.DatabaseKeys.MAX_SUBSCRIPTIONS
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseDefinitions
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.firebase.notification.FirebaseNotification
import io.stanc.pogoradar.firebase.notification.NotificationContent
import io.stanc.pogoradar.firebase.notification.NotificationHolder
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.map.ClusterManager
import io.stanc.pogoradar.map.MapGridProvider
import io.stanc.pogoradar.subscreen.BaseMapFragment
import io.stanc.pogoradar.subscreen.ZoomLevel
import io.stanc.pogoradar.utils.ParcelableDataFragment.Companion.PARCELABLE_EXTRA_DATA_OBJECT
import io.stanc.pogoradar.utils.WaitingSpinner


class MapInteractionFragment: Fragment() {

    private var mapGridProvider: MapGridProvider? = null
    private var clusterManager: ClusterManager? = null
    private val database = FirebaseDatabase()

    private var map: GoogleMap? = null
    private var poiImage: ImageView? = null
    private var actionButtonLayout: View? = null
    private var negativeButton: ImageView? = null

    private var mapFragment: BaseMapFragment? = null
    private var famButton: FloatingActionsMenu? = null
    private var pendingNotification: NotificationContent? = null
    private var lastMarkerClicked: Marker? = null

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMapInteractionBinding.inflate(inflater, container, false)
        binding.settings = MapFilterSettings
        val rootLayout = binding.root

        setupMapFragment()

        // warning info label
        rootLayout.findViewById<TextView>(R.id.warning_info_text)?.text = getText(R.string.app_info_map_filter_active)

        // floating action buttons
        setupFAB(rootLayout)

        // poi & subscriptions
        setupButtons(rootLayout)

        return rootLayout
    }

    override fun onResume() {
        super.onResume()

        FirebaseDefinitions.loadDefinitions(database)

        NotificationHolder.consumeNotification()?.let { notification ->
            Log.i(TAG, "onNotificationReceived: $notification")
            pendingNotification =  notification
            tryToConsumeNotification()
        }

        loadMapItems()
    }

    private fun tryToConsumeNotification() {
        mapFragment?.let {

            pendingNotification?.let { notification ->
                val geoHashStartPosition = GeoHash(notification.latitude, notification.longitude)
//                Log.d(TAG, "consume notification: $pendingNotification with position: $geoHashStartPosition")
                it.updateCameraPosition(geoHashStartPosition, ZoomLevel.STREET)
            }
            pendingNotification = null
        }
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_mapview) as BaseMapFragment
        tryToConsumeNotification()
        mapFragment?.setDelegate(object : BaseMapFragment.MapDelegate {

            override fun onMapReady(googleMap: GoogleMap) {

                googleMap.setOnMapClickListener(onMapClickListener)
                googleMap.setOnMarkerClickListener(onMarkersClickListener)
                googleMap.setOnCameraIdleListener(onCameraIdleListener)
                googleMap.uiSettings.isCompassEnabled = true
                googleMap.uiSettings.setAllGesturesEnabled(true)

                context?.let {

                    clusterManager = ClusterManager(it, googleMap, object: ClusterManager.MarkerDelegate {

                        override fun onArenaInfoWindowClicked(arena: FirebaseArena) {
                            showArenaFragment(arena)
                        }

                        override fun onPokestopInfoWindowClicked(pokestop: FirebasePokestop) {
                            showPokestopFragment(pokestop)
                        }
                    })

                    FirebaseDefinitions.loadDefinitions(database)

                    mapGridProvider = MapGridProvider(googleMap, it)
                    mapGridProvider?.showGeoHashGridList()
                }

                map = googleMap
            }

            override fun onCameraStartAnimationFinished() {
                // TODO("not implemented"), needed?
            }
        })
    }

    /**
     * map listener
     */

    private val onMapClickListener = GoogleMap.OnMapClickListener {

        if (currentMode == MapMode.EDIT_PUSH_REGISTRATION) {
            toggleSubscriptions(GeoHash(it))
        }
    }

    private val onMarkersClickListener = GoogleMap.OnMarkerClickListener {

        lastMarkerClicked = it

        if (currentMode == MapMode.EDIT_PUSH_REGISTRATION) {
            toggleSubscriptions(GeoHash(it.position))
            true
        } else {
            false
        }
    }

    private var lastGeoHashMatrix: List<GeoHash> = emptyList()

    private val onCameraIdleListener = GoogleMap.OnCameraIdleListener {

        val zoomLevel = map?.cameraPosition?.zoom
        Log.d(TAG, "Debug:: current zoom level: $zoomLevel")

        // TODO: loading bugs:
        // 1. nachdem der User von einer Arena/einem Pokestop zurück auf die Karte navigiert (z.B. nachdem er einen raid erstellt hat) wird die Arena/Pokestop nicht aktualisiert. Weil hierfür ein "event" fehlt.
        // 2. je nach zoom und bewegung auf der Karte werden sehr viele items auf einmal angezeigt -> performance problem


        map?.cameraPosition?.zoom?.let { currentZoomValue ->
            if (currentZoomValue >= ZoomLevel.DISTRICT.value) {
                loadMapItems()
            }
        }

        clusterManager?.onCameraIdle()
    }

    private fun loadMapItems() {

        mapFragment?.visibleRegionBounds()?.let { bounds ->
            GeoHash.geoHashMatrix(bounds.northeast, bounds.southwest)?.let { newGeoHashMatrix ->

//                clusterManager?.removeAllArenas()
//                clusterManager?.removeAllPokestops()

                if (!isSameGeoHashList(newGeoHashMatrix, lastGeoHashMatrix)) {

                    database.loadPokestops(newGeoHashMatrix) { pokestopList ->
                        pokestopList?.let { clusterManager?.showPokestopsAndRemoveOldOnes(it) }
                    }

                    database.loadArenas(newGeoHashMatrix) { arenaList ->
                        arenaList?.let { clusterManager?.showArenasAndRemoveOldOnes(it) }
                    }

                    lastGeoHashMatrix = newGeoHashMatrix
                }

            } ?: run {
                Log.w(TAG, "max size of GeoHash matrix reached!")
            }
        }
    }

    private fun isSameGeoHashList(list1: List<GeoHash>, list2: List<GeoHash>): Boolean {
        return list1.containsAll(list2) && list2.containsAll(list1)
    }

    /**
     * map
     */

    private enum class MapMode {
        DEFAULT,
        EDIT_PUSH_REGISTRATION,
        SET_NEW_POI
    }

    private var currentMode = MapMode.DEFAULT

    private fun toggleSubscriptions(geoHash: GeoHash) {

        database.formattedFirebaseGeoHash(geoHash)?.let { geoHash ->
            mapGridProvider?.let {
                if (it.geoHashExists(geoHash)) {
                    removeSubscription(it, geoHash)
                } else {
                    addSubscription(it, geoHash)
                }
            }
        }
    }

    // TODO: ??? not sending each subscription one by one, but send all new and removed at once, after edit mode is over ???
    private fun addSubscription(mapGridProvider: MapGridProvider, geoHash: GeoHash) {

        if (mapGridProvider.geoHashes().size < MAX_SUBSCRIPTIONS) {
            FirebaseNotification.subscribeToArea(geoHash) { successful ->
                if (!successful) {
                    Popup.showToast(context, R.string.exceptions_subscription_sending_failed)
                }
            }
            mapGridProvider.showGeoHashGrid(geoHash)
        } else {
            Popup.showToast(context, R.string.map_max_subscriptions)
        }
    }

    private fun removeSubscription(mapGridProvider: MapGridProvider, geoHash: GeoHash) {

        FirebaseNotification.unsubscribeFromArea(geoHash) { successful ->
            if (!successful) {
                Popup.showToast(context, R.string.exceptions_subscription_sending_failed)
            }
        }
        mapGridProvider.removeGeoHashGrid(geoHash)
    }

    /**
     * arena & pokestop fragments
     */

    private fun showArenaFragment(arena: FirebaseArena) {
        resetModes()
        val bundle = Bundle().apply {
            this.putParcelable(PARCELABLE_EXTRA_DATA_OBJECT, arena)
        }
        findNavController().navigate(R.id.action_blankFragment_to_arenaFragment, bundle)
    }

    private fun showPokestopFragment(pokestop: FirebasePokestop) {
        resetModes()
        val bundle = Bundle().apply {
            this.putParcelable(PARCELABLE_EXTRA_DATA_OBJECT, pokestop)
        }
        findNavController().navigate(R.id.action_blankFragment_to_pokestopFragment, bundle)
    }

    private fun showMapItemCreationFragment(latLng: LatLng) {
        resetModes()
        val bundle = Bundle().apply {
            this.putParcelable(PARCELABLE_EXTRA_DATA_OBJECT, latLng)
        }
        findNavController().navigate(R.id.action_blankFragment_to_mapItemCreationFragment, bundle)
    }

    private fun showMapSettingsFragment() {
        resetModes()
        findNavController().navigate(R.id.action_blankFragment_to_mapSettingsFragment)
    }

    /**
     * view setup
     */

    private fun setupFAB(rootLayout: View) {

        famButton = rootLayout.findViewById(R.id.fab_menu)
        famButton?.setOnFloatingActionsMenuUpdateListener(object: FloatingActionsMenu.OnFloatingActionsMenuUpdateListener{
            override fun onMenuCollapsed() {
                resetModes()
            }

            override fun onMenuExpanded() {}
        })

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_push_registration)?.setOnClickListener {
            startModePushRegistration()
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_map_type)?.setOnClickListener {
            map?.mapType = nextMapType()
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_new_poi)?.setOnClickListener {
            startModeNewPoi()
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_map_filter)?.setOnClickListener {
            showMapSettingsFragment()
        }
    }

    private fun setupButtons(rootLayout: View) {

        poiImage = rootLayout.findViewById(R.id.fragment_map_imageview_poi)

        actionButtonLayout = rootLayout.findViewById(R.id.fragment_map_layout_buttons)

        actionButtonLayout?.findViewById<ImageButton>(R.id.fragment_map_button_positive)?.setOnClickListener {
            when(currentMode) {

                MapMode.SET_NEW_POI -> {
                    mapFragment?.centeredPosition()?.let { latlng ->
                        showMapItemCreationFragment(latlng)
                    }
                }

                MapMode.EDIT_PUSH_REGISTRATION -> {
                    resetModes()
                }

                else -> {}
            }
        }

        actionButtonLayout?.findViewById<ImageButton>(R.id.fragment_map_button_neutral)?.setOnClickListener {

            when(currentMode) {

                MapMode.SET_NEW_POI, MapMode.EDIT_PUSH_REGISTRATION  -> {
                    resetModes()
                }

                else -> {}
            }
        }

        negativeButton = actionButtonLayout?.findViewById(R.id.fragment_map_button_negative)
        negativeButton?.setOnClickListener {

            when(currentMode) {

                MapMode.EDIT_PUSH_REGISTRATION -> {

                    WaitingSpinner.showProgress(R.string.spinner_title_loading_map_data)
                    FirebaseNotification.unsubscribeFromAllAreas(onCompletionCallback = { successful ->
                        WaitingSpinner.hideProgress()
                        if (successful) {
                            mapGridProvider?.clearGeoHashGridList()
                        } else {
                            Popup.showToast(context, R.string.exceptions_subscription_sending_failed)
                        }
                    })
                }

                else -> {}
            }
        }
    }

    /**
     * map modes
     */

    private fun startModePushRegistration() {
        if (currentMode != MapMode.EDIT_PUSH_REGISTRATION) {
            resetModes()
            actionButtonLayout?.visibility = View.VISIBLE
            negativeButton?.visibility = View.VISIBLE
            currentMode = MapMode.EDIT_PUSH_REGISTRATION
            lastMarkerClicked?.hideInfoWindow()
            famButton?.visibility = View.INVISIBLE

            WaitingSpinner.showProgress(R.string.spinner_title_loading_map_data)
            FirebaseNotification.requestAreaSubscriptions { geoHashes ->

                geoHashes?.let {
                    for (geoHash in geoHashes) {
                        mapGridProvider?.showGeoHashGrid(geoHash)
                    }
                }

                WaitingSpinner.hideProgress()
                Popup.showInfo(context, title = R.string.popup_info_push_registration_title, description = R.string.popup_info_push_registration_message)
            }
        }
    }

    private fun startModeNewPoi() {
        if (currentMode != MapMode.SET_NEW_POI) {
            resetModes()
            poiImage?.visibility = View.VISIBLE
            actionButtonLayout?.visibility = View.VISIBLE
            currentMode = MapMode.SET_NEW_POI
            lastMarkerClicked?.hideInfoWindow()
            famButton?.visibility = View.INVISIBLE
            mapFragment?.zoomTo(ZoomLevel.STREET, true)
        }
    }

    private fun resetModes() {
        poiImage?.visibility = View.INVISIBLE
        actionButtonLayout?.visibility = View.INVISIBLE
        negativeButton?.visibility = View.INVISIBLE
        famButton?.visibility = View.VISIBLE

//        if (currentMode == MapMode.EDIT_PUSH_REGISTRATION) {
//
//            mapGridProvider?.let {
//
//                Log.i(TAG, "Debug:: resetModes ...")
//                WaitingSpinner.showProgress(R.string.spinner_title_map_data)
//
//                database.setSubscriptionsForPush(it.geoHashes()) { taskSuccessful ->
//                    Log.i(TAG, "Debug:: setSubscriptionsForPush finished, taskSuccessful: $taskSuccessful")
//
//                    mapGridProvider?.clearGeoHashGridList()
//                    WaitingSpinner.hideProgress()
//                }
//            }
//        }
        mapGridProvider?.clearGeoHashGridList()
        currentMode = MapMode.DEFAULT
        famButton?.collapse()
    }

    private fun nextMapType(): Int {
        return when(map?.mapType) {
            GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
            GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_HYBRID
            GoogleMap.MAP_TYPE_HYBRID -> GoogleMap.MAP_TYPE_NORMAL
            else -> GoogleMap.MAP_TYPE_NORMAL
        }
    }

    companion object {
        private val TAG = this::class.java.name
    }
}