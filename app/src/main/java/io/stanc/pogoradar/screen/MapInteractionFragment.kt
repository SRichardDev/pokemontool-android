package io.stanc.pogoradar.screen

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.stanc.pogoradar.Popup
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.MAX_SUBSCRIPTIONS
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseDefinitions
import io.stanc.pogoradar.firebase.NotificationContent
import io.stanc.pogoradar.firebase.NotificationHolder
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
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
    private var firebase: FirebaseDatabase? = null

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
        val rootLayout = inflater.inflate(R.layout.fragment_map_grid, container, false)
        setupMapFragment()

        // floating action buttons
        setupFAB(rootLayout)

        // poi & subscriptions
        setupButtons(rootLayout)

        return rootLayout
    }

    override fun onResume() {
        super.onResume()
        firebase?.let { FirebaseDefinitions.loadDefinitions(it) }

        NotificationHolder.consumeNotification()?.let { notification ->
            Log.i(TAG, "onNotificationReceived: $notification")
            pendingNotification =  notification
            tryToConsumeNotification()
        }
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
                    ClusterManager(it, googleMap, object: ClusterManager.MarkerDelegate {
                        override fun onArenaInfoWindowClicked(arena: FirebaseArena) {
                            showArenaFragment(arena)
                        }

                        override fun onPokestopInfoWindowClicked(pokestop: FirebasePokestop) {
                            showPokestopFragment(pokestop)
                        }

                    }).let { manager ->
                        firebase = FirebaseDatabase(manager.pokestopDelegate, manager.arenaDelegate)
                        firebase?.let { FirebaseDefinitions.loadDefinitions(it) }
                        clusterManager = manager
                    }

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

        mapFragment?.visibleRegionBounds()?.let { bounds ->
            GeoHash.geoHashMatrix(bounds.northeast, bounds.southwest)?.let { newGeoHashMatrix ->

                if (!isSameGeoHashList(newGeoHashMatrix, lastGeoHashMatrix)) {

                    newGeoHashMatrix.forEach { geoHash ->
                        firebase?.loadPokestops(geoHash)
                        firebase?.loadArenas(geoHash)
                    }

                    lastGeoHashMatrix = newGeoHashMatrix
                }

            } ?: run {
                Log.v(TAG, "Max zooming level reached!")
            }
        }

        clusterManager?.onCameraIdle()
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

        firebase?.formattedFirebaseGeoHash(geoHash)?.let { geoHash ->
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
            firebase?.addSubscriptionForPush(geoHash) { successful ->
                if (!successful) {
                    Toast.makeText(context, R.string.exceptions_subscription_sending_failed, Toast.LENGTH_LONG).show()
                }
            }
            mapGridProvider.showGeoHashGrid(geoHash)
        } else {
            Toast.makeText(context, R.string.map_max_subscriptions, Toast.LENGTH_LONG).show()
        }
    }

    private fun removeSubscription(mapGridProvider: MapGridProvider, geoHash: GeoHash) {

        firebase?.removePushSubscription(geoHash) { successful ->
            if (!successful) {
                Toast.makeText(context, R.string.exceptions_subscription_sending_failed, Toast.LENGTH_LONG).show()
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
                    firebase?.removeAllPushSubscriptions(onCompletionCallback = { successful ->
                        WaitingSpinner.hideProgress()
                        if (successful) {
                            mapGridProvider?.clearGeoHashGridList()
                        } else {
                            Toast.makeText(context, R.string.exceptions_subscription_sending_failed, Toast.LENGTH_LONG).show()
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

            WaitingSpinner.showProgress(R.string.spinner_title_loading_map_data)
            firebase?.loadSubscriptions { geoHashes ->

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
            mapFragment?.zoomTo(ZoomLevel.STREET, true)
        }
    }

    private fun resetModes() {
        poiImage?.visibility = View.INVISIBLE
        actionButtonLayout?.visibility = View.INVISIBLE
        negativeButton?.visibility = View.INVISIBLE

//        if (currentMode == MapMode.EDIT_PUSH_REGISTRATION) {
//
//            mapGridProvider?.let {
//
//                Log.i(TAG, "Debug:: resetModes ...")
//                WaitingSpinner.showProgress(R.string.spinner_title_map_data)
//
//                firebase?.setSubscriptionsForPush(it.geoHashes()) { taskSuccessful ->
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