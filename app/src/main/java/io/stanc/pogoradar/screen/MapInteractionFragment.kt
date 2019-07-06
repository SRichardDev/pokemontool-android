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
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.firebase.DatabaseKeys.LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.MAX_SUBSCRIPTIONS
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseDefinitions
import io.stanc.pogoradar.firebase.NotificationService
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.map.ClusterManager
import io.stanc.pogoradar.map.MapGridProvider
import io.stanc.pogoradar.subscreen.MapFragment
import io.stanc.pogoradar.subscreen.ZoomLevel
import io.stanc.pogoradar.utils.PermissionManager
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.utils.WaitingSpinner



class MapInteractionFragment: Fragment() {

    private var mapGridProvider: MapGridProvider? = null
    private var clusterManager: ClusterManager? = null
    private var firebase: FirebaseDatabase? = null

    private var map: GoogleMap? = null
    private var poiImage: ImageView? = null
    private var actionButtonLayout: View? = null

    private var mapFragment: MapFragment? = null
    private var famButton: FloatingActionsMenu? = null

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_map_grid, container, false)
        Log.d(TAG, "Debug:: onCreateView()")
        setupMapFragment()

        // floating action buttons
        setupFAB(rootLayout)

        // poi
        poiImage = rootLayout.findViewById(R.id.fragment_map_imageview_poi)

        // button layout
        setupNewPoiViews(rootLayout)

        return rootLayout
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "Debug:: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Debug:: onResume()")
        AppbarManager.setTitle(getString(R.string.default_app_title))
        firebase?.let { FirebaseDefinitions.loadDefinitions(it) }
        NotificationService.consumeNotification()?.let { notification ->
            Log.d(TAG, "Debug:: onResume(), consumeNotification: $notification")
            val geoHashStartPosition = GeoHash(notification.latitude, notification.longitude)
            Log.d(TAG, "Debug:: onResume(), try to update: ${geoHashStartPosition.toLatLng()}")
            mapFragment?.updateCameraPosition(geoHashStartPosition)
        }
    }
    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_mapview) as MapFragment
        Log.d(TAG, "Debug:: setupMapFragment...")
        mapFragment?.setDelegate(object : MapFragment.MapDelegate {

            override fun onMapReady(googleMap: GoogleMap) {

                googleMap.setOnMapLongClickListener(onMapLongClickListener)
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
                }

                mapGridProvider = MapGridProvider(googleMap)
                mapGridProvider?.showGeoHashGridList()

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

    private val onMapLongClickListener = GoogleMap.OnMapLongClickListener {

        if (PermissionManager.isLocationPermissionGranted(context)) {
            if (currentMode == MapMode.EDIT_PUSH_REGISTRATION) {

                firebase?.formattedFirebaseGeoHash(GeoHash(it))?.let { geoHash ->
                    toggleSubscriptions(geoHash)
                }
            }
        } else {
            Toast.makeText(context, R.string.dialog_location_disabled_message, Toast.LENGTH_LONG).show()
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
                Log.w(TAG, "Max zooming level reached!")
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

        mapGridProvider?.let {
            if (it.geoHashExists(geoHash)) {
                removeSubscription(it, geoHash)
            } else {
                addSubscription(it, geoHash)
            }
        }
    }

    private fun addSubscription(mapGridProvider: MapGridProvider, geoHash: GeoHash) {

        if (mapGridProvider.geoHashes().size < MAX_SUBSCRIPTIONS) {
            firebase?.subscribeForPush(geoHash) { successful ->
                if (successful) {
                    mapGridProvider.showGeoHashGrid(geoHash)
                } else {
                    Toast.makeText(context, R.string.exceptions_subscription_sending_failed, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, R.string.map_max_subscriptions, Toast.LENGTH_LONG).show()
        }
    }

    private fun removeSubscription(mapGridProvider: MapGridProvider, geoHash: GeoHash) {

        firebase?.removeSubscription(geoHash) { successful ->
            if (successful) {
                mapGridProvider.removeGeoHashGrid(geoHash)
            } else {
                Toast.makeText(context, R.string.exceptions_subscription_sending_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * arena & pokestop fragments
     */

    private fun showArenaFragment(arena: FirebaseArena) {
        resetInteractionMap()
        val fragment = ArenaFragment.newInstance(arena)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    private fun showPokestopFragment(pokestop: FirebasePokestop) {
        resetInteractionMap()
        val fragment = PokestopFragment.newInstance(pokestop)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    private fun showMapItemCreationFragment(latLng: LatLng) {
        resetInteractionMap()
        val fragment = MapItemCreationFragment.newInstance(latLng)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    private fun showMapSettingsFragment() {
        resetInteractionMap()
        val fragment = MapSettingsFragment()
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    /**
     * FABs
     */

    private fun setupFAB(rootLayout: View) {

        famButton = rootLayout.findViewById(R.id.fab_menu)
        famButton?.setOnFloatingActionsMenuUpdateListener(object: FloatingActionsMenu.OnFloatingActionsMenuUpdateListener{
            override fun onMenuCollapsed() {
                resetInteractionMap()
            }

            override fun onMenuExpanded() {}
        })

        // TODO: refactor subscriptions !
        rootLayout.findViewById<FloatingActionButton>(R.id.fab_push_registration)?.setOnClickListener {

            mapGridProvider?.clearGeoHashGridList()
            currentMode = MapMode.EDIT_PUSH_REGISTRATION

            WaitingSpinner.showProgress(R.string.spinner_title_map_data)
            firebase?.loadSubscriptions { geoHashes ->
                Log.i(TAG, "loading subscriptions for geoHashes: $geoHashes")

                geoHashes?.let {
                    for (geoHash in geoHashes) {
                        mapGridProvider?.showGeoHashGrid(geoHash)
                    }
                }

                WaitingSpinner.hideProgress()
            }
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_map_type)?.setOnClickListener {
            map?.mapType = nextMapType()
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_new_poi)?.setOnClickListener {
            resetInteractionMap()
            showNewPoiIcon()
            currentMode = MapMode.SET_NEW_POI
            mapFragment?.zoomTo(ZoomLevel.STREET, true)
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_map_filter)?.setOnClickListener {
            showMapSettingsFragment()
        }
    }

    private fun setupNewPoiViews(rootLayout: View) {
        actionButtonLayout = rootLayout.findViewById(R.id.fragment_map_layout_buttons)
        actionButtonLayout?.findViewById<ImageButton>(R.id.fragment_map_button_positive)?.setOnClickListener {
            when(currentMode) {

                MapMode.SET_NEW_POI -> {
                    mapFragment?.centeredPosition()?.let { latlng ->
                        showMapItemCreationFragment(latlng)
                    }
                }

                else -> {}
            }
        }
        actionButtonLayout?.findViewById<ImageButton>(R.id.fragment_map_button_negative)?.setOnClickListener {
            resetInteractionMap()
        }
    }

    private fun resetInteractionMap() {
        dismissNewPoiIcon()
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

    /**
     * poi
     */

    private fun showNewPoiIcon() {
        poiImage?.visibility = View.VISIBLE
        actionButtonLayout?.visibility = View.VISIBLE
    }

    private fun dismissNewPoiIcon() {
        poiImage?.visibility = View.GONE
        actionButtonLayout?.visibility = View.GONE
    }

    companion object {
        private val TAG = this::class.java.name
    }
}