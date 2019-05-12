package io.stanc.pogotool

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.ClusterManager
import io.stanc.pogotool.map.MapGridProvider
import io.stanc.pogotool.map.RaidBossImageMapper
import io.stanc.pogotool.utils.PermissionManager
import io.stanc.pogotool.utils.ShowFragmentManager
import io.stanc.pogotool.utils.WaitingSpinner


class MapInteractionFragment: Fragment() {

    private var mapGridProvider: MapGridProvider? = null
    private var clusterManager: ClusterManager? = null
    private var firebase: FirebaseDatabase? = null

    private var crosshairImage: ImageView? = null
    private var crosshairAnimation: Animation? = null

    private var mapFragment: MapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.intent?.extras?.let { bundle ->

            Log.d(TAG, "Debug:: Intent has extras [bundle.containsKey(\"${FirebaseDatabase.NOTIFICATION_DATA_LONGITUDE}\"): ${bundle.containsKey(
                FirebaseDatabase.NOTIFICATION_DATA_LONGITUDE
            )}, bundle.containsKey(\"${FirebaseDatabase.NOTIFICATION_DATA_LATITUDE}\"): ${bundle.containsKey(
                FirebaseDatabase.NOTIFICATION_DATA_LATITUDE
            )}]")

            if (bundle.containsKey(FirebaseDatabase.NOTIFICATION_DATA_LATITUDE) && bundle.containsKey(FirebaseDatabase.NOTIFICATION_DATA_LONGITUDE)) {

                val latitude = (bundle.get(FirebaseDatabase.NOTIFICATION_DATA_LATITUDE) as String).toDouble()
                val longitude = (bundle.get(FirebaseDatabase.NOTIFICATION_DATA_LONGITUDE) as String).toDouble()

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

        // crosshair
        crosshairAnimation = AnimationUtils.loadAnimation(context, R.anim.flashing)
        crosshairImage = rootLayout.findViewById(R.id.fragment_map_imageview_crosshair)

        return rootLayout
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(getString(R.string.app_name))
        firebase?.let { RaidBossImageMapper.loadRaidBosses(it) }
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_mapview) as MapFragment
        mapFragment?.setDelegate(object : MapFragment.MapDelegate {

            override fun onMapReady(googleMap: GoogleMap) {

                googleMap.setOnMapClickListener(onMapClickListener)
                googleMap.setOnMapLongClickListener(onMapLongClickListener)
                googleMap.setOnCameraIdleListener(onCameraIdleListener)

                context?.let {
                    ClusterManager(it, googleMap, object: ClusterManager.MarkerDelegate {
                        override fun onArenaInfoWindowClicked(arena: FirebaseArena) {
                            showArenaFragment(arena)
                        }

                        override fun onPokestopInfoWindowClicked(pokestop: FirebasePokestop) {
                            Log.i(TAG, "Debug:: onPokestopInfoWindowClicked($pokestop)")
                        }

                    }).let { manager ->
                        firebase = FirebaseDatabase(manager.pokestopDelegate, manager.arenaDelegate)
                        firebase?.let { RaidBossImageMapper.loadRaidBosses(it) }
                        clusterManager = manager
                    }
                }

                mapGridProvider = MapGridProvider(googleMap)
                mapGridProvider?.showGeoHashGridList()
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

        when(currentMode) {

            MapMode.NEW_ARENA, MapMode.NEW_POKESTOP -> {
                mapFragment?.centeredPosition()?.let { latlng ->
                    showMapItemFragment(currentMode, latlng)
                }
            }

            else -> {}
        }
    }

    private val onMapLongClickListener = GoogleMap.OnMapLongClickListener {

        if (PermissionManager.isLocationPermissionGranted(context)) {
            if (currentMode == MapMode.EDIT_GEO_HASHES) {

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

    enum class MapMode {
        DEFAULT,
        EDIT_GEO_HASHES,
        NEW_ARENA,
        NEW_POKESTOP
    }
    private var currentMode = MapMode.DEFAULT

    private fun showMapItemFragment(mapMode: MapMode, latLng: LatLng) {

        val fragment = MapItemFragment.newInstance(mapMode, latLng)
        Log.d(TAG, "Debug:: showMapItemFragment(${currentMode.name}, $latLng)")
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    private fun toggleSubscriptions(geoHash: GeoHash) {

        mapGridProvider?.let {
            if (it.geoHashExists(geoHash)) {

                // TODO: add onCompletionCallBack for removing ...
                Log.i(TAG, "Debug:: remove subscription and geoHashGrid for $geoHash...")
                firebase?.removeSubscription(geoHash)
                it.removeGeoHashGrid(geoHash)

            } else {

                Log.i(TAG, "Debug:: add subscription and geoHashGrid for $geoHash...")
                if (it.geoHashes().size < FirebaseDatabase.MAX_SUBSCRIPTIONS) {
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
        val fragment = ArenaFragment.newInstance(arena)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    /**
     * FABs
     */

    private fun setupFAB(rootLayout: View) {

        rootLayout.findViewById<FloatingActionsMenu>(R.id.fab_menu)?.setOnFloatingActionsMenuUpdateListener(object: FloatingActionsMenu.OnFloatingActionsMenuUpdateListener{
            override fun onMenuCollapsed() {
                resetMap()
            }

            override fun onMenuExpanded() {
                resetMap()
            }

            private fun resetMap() {
                dismissCrosshair()
                mapGridProvider?.clearGeoHashGridList()
                currentMode = MapMode.DEFAULT
            }
        })

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_edit_geo_hashs)?.let { fab ->
            fab.setOnClickListener {

                dismissCrosshair()
                mapGridProvider?.clearGeoHashGridList()
                currentMode = MapMode.EDIT_GEO_HASHES

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
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_arena)?.let { fab ->
            fab.setOnClickListener {

                showCrosshair()
                currentMode = MapMode.NEW_ARENA
            }
        }

        rootLayout.findViewById<FloatingActionButton>(R.id.fab_pokestop)?.let { fab ->
            fab.setOnClickListener {

                showCrosshair()
                currentMode = MapMode.NEW_POKESTOP
            }
        }
    }

    /**
     * Crosshair
     */

    private fun showCrosshair() {
        crosshairImage?.visibility = View.VISIBLE
        crosshairImage?.startAnimation(crosshairAnimation)

    }

    private fun dismissCrosshair() {
        crosshairAnimation?.cancel()
        crosshairAnimation?.reset()
        crosshairImage?.clearAnimation()
        crosshairImage?.visibility = View.GONE
    }

    companion object {
        private val TAG = this::class.java.name
    }
}