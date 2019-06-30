package io.stanc.pogotool.screen

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
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.DatabaseKeys.LATITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.LONGITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.MAX_SUBSCRIPTIONS
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDefinitions
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.ClusterManager
import io.stanc.pogotool.map.MapGridProvider
import io.stanc.pogotool.subscreen.MapFragment
import io.stanc.pogotool.utils.PermissionManager
import io.stanc.pogotool.utils.ShowFragmentManager
import io.stanc.pogotool.utils.WaitingSpinner


class MapInteractionFragment: Fragment() {

    private var mapGridProvider: MapGridProvider? = null
    private var clusterManager: ClusterManager? = null
    private var firebase: FirebaseDatabase? = null

    private var map: GoogleMap? = null
    private var poiImage: ImageView? = null
    private var actionButtonLayout: View? = null

    private var mapFragment: MapFragment? = null
    private var famButton: FloatingActionsMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.intent?.extras?.let { bundle ->

            Log.d(
                TAG, "Debug:: Intent has extras [bundle.containsKey(\"$LONGITUDE\"): ${bundle.containsKey(
                LONGITUDE
            )}, bundle.containsKey(\"$LATITUDE\"): ${bundle.containsKey(
                LATITUDE
            )}]")

            if (bundle.containsKey(LATITUDE) && bundle.containsKey(LONGITUDE)) {

                val latitude = (bundle.get(LATITUDE) as String).toDouble()
                val longitude = (bundle.get(LONGITUDE) as String).toDouble()

                Log.i(TAG, "Debug:: onCreate() NOTIFICATION: latitude: $latitude, longitude: $longitude")
                mapFragment?.setNextStartPosition(latitude, longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_map_grid, container, false)

        setupMapFragment()

        // floating action buttons
        setupFAB(rootLayout)

        // poi
        poiImage = rootLayout.findViewById(R.id.fragment_map_imageview_poi)

        // button layout
        setupNewPoiViews(rootLayout)

        return rootLayout
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(getString(R.string.default_app_title))
        firebase?.let { FirebaseDefinitions.loadDefinitions(it) }
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_mapview) as MapFragment
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

            } ?: kotlin.run {
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

                // TODO: add onCompletionCallBack for removing ...
                Log.i(TAG, "Debug:: remove subscription and geoHashGrid for $geoHash...")
                firebase?.removeSubscription(geoHash)
                it.removeGeoHashGrid(geoHash)

            } else {

                Log.i(TAG, "Debug:: add subscription and geoHashGrid for $geoHash...")
                if (it.geoHashes().size < MAX_SUBSCRIPTIONS) {
                    firebase?.subscribeForPush(geoHash) { successful ->
                        Log.i(TAG, "Debug:: subscribeForPush($geoHash), successful: $successful")
                        it.showGeoHashGrid(geoHash)
                    }
                } else {
                    Toast.makeText(context, R.string.map_max_subscriptions, Toast.LENGTH_LONG).show()
                }
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