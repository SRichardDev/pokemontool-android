package io.stanc.pogotool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.MAX_SUBSCRIPTIONS
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.NOTIFICATION_DATA_LATITUDE
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.NOTIFICATION_DATA_LONGITUDE
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.ClusterManager
import io.stanc.pogotool.map.MapGridProvider
import io.stanc.pogotool.utils.PermissionManager
import io.stanc.pogotool.utils.WaitingSpinner


class MapFragment: Fragment() {

    // map manager
    private var locationManager: io.stanc.pogotool.map.LocationManager? = null
    private var mapGridProvider: MapGridProvider? = null
    private var clusterManager: ClusterManager? = null
    private var firebase: FirebaseDatabase? = null

    // map view
    private var mapView: MapView? = null
    private var crosshairImage: ImageView? = null
    private var crosshairAnimation: Animation? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.intent?.extras?.let { bundle ->

            Log.d(TAG, "Debug:: Intent has extras [bundle.containsKey(\"$NOTIFICATION_DATA_LONGITUDE\"): ${bundle.containsKey(NOTIFICATION_DATA_LONGITUDE)}, bundle.containsKey(\"$NOTIFICATION_DATA_LATITUDE\"): ${bundle.containsKey(NOTIFICATION_DATA_LATITUDE)}]")

            if (bundle.containsKey(NOTIFICATION_DATA_LATITUDE) && bundle.containsKey(NOTIFICATION_DATA_LONGITUDE)) {

                val latitude = (bundle.get(NOTIFICATION_DATA_LATITUDE) as String).toDouble()
                val longitude = (bundle.get(NOTIFICATION_DATA_LONGITUDE) as String).toDouble()

                locationManager?.setNextStartLocation(latitude, longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentLayout = inflater.inflate(R.layout.layout_fragment_map, container, false)

        mapView = fragmentLayout.findViewById(R.id.fragment_map_mapview) as MapView
        mapView?.onCreate(savedInstanceState)
        mapView?.onResume() // needed to get the map to display immediately

        activity?.let { MapsInitializer.initialize(it.applicationContext) }

        mapView?.getMapAsync { setupMap(it) }

        // floating action buttons
        setupFAB(fragmentLayout)

        // crosshair
        crosshairAnimation = AnimationUtils.loadAnimation(context, R.anim.flashing)
        crosshairImage = fragmentLayout.findViewById(R.id.fragment_map_imageview_crosshair)

        return fragmentLayout
    }

    private fun setupMap(googleMap: GoogleMap) {

        context?.let { context ->
            locationManager = io.stanc.pogotool.map.LocationManager(googleMap, context)

            locationManager?.setOnMapClickListener(onMapClickListener)
            locationManager?.setOnMapLongClickListener(onMapLongClickListener)
            locationManager?.setOnCameraIdleListener(onCameraIdleListener)
            locationManager?.setOnMarkerClickListener(onMarkerClickListener)

            clusterManager = ClusterManager(context, googleMap)
            clusterManager?.let {
                firebase = FirebaseDatabase(it.pokestopDelegate, it.arenaDelegate)
            }
        }

        // init view
        locationManager?.focusStartLocation(activity)

        mapGridProvider = MapGridProvider(googleMap)
        mapGridProvider?.showGeoHashGridList()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        activity?.let { PermissionManager.checkLocationPermission(it) }

        locationManager?.let {
            if (!it.isGPSEnabled()) {
                showAlertMessageNoGPS()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "onLowMemory()")
        mapView?.onLowMemory()
    }

    /**
     * map
     */

    enum class Mode {
        DEFAULT,
        EDIT_GEO_HASHES,
        NEW_ARENA,
        NEW_POKESTOP
    }
    private var currentMode = Mode.DEFAULT

    private val onMapClickListener = GoogleMap.OnMapClickListener {

        when(currentMode) {

            Mode.NEW_ARENA -> {
                FirebaseServer.currentUser?.name?.let { submitter ->
                    locationManager?.centeredPosition()?.let { latlng ->

                        // TODO: user should type name
                        val geoHash = GeoHash(latlng.latitude, latlng.longitude)
                        val arena = FirebaseArena("", "new Debug Arena", geoHash, submitter)
                        firebase?.pushArena(arena)
                    }
                }
            }

            Mode.NEW_POKESTOP -> {
                FirebaseServer.currentUser?.name?.let { submitter ->
                    locationManager?.centeredPosition()?.let { latlng ->

                        // TODO: user should type name
                        val geoHash = GeoHash(latlng.latitude, latlng.longitude)
                        val pokestop = FirebasePokestop("", "new Debug Pokestop", geoHash, submitter)
                        firebase?.pushPokestop(pokestop)
                    }
                }
            }

            else -> {}
        }
    }

    private val onMapLongClickListener = GoogleMap.OnMapLongClickListener {

        if (PermissionManager.isLocationPermissionGranted(context)) {
            if (currentMode == Mode.EDIT_GEO_HASHES) {

                firebase?.formattedFirebaseGeoHash(GeoHash(it))?.let { geoHash ->
                    toggleSubscriptions(geoHash)
                }
            }
        } else {
            Toast.makeText(context, R.string.dialog_location_disabled_message, LENGTH_LONG).show()
        }
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
                if (it.geoHashes().size < MAX_SUBSCRIPTIONS) {
                    firebase?.subscribeForPush(geoHash) { successful ->
                        Log.i(TAG, "Debug:: subscribeForPush($geoHash), successful: $successful")
                        it.showGeoHashGrid(geoHash)
                    }
                } else {
                    Toast.makeText(context, R.string.map_max_subscriptions, LENGTH_LONG).show()
                }
            }
        }
    }

    private val onCameraIdleListener = GoogleMap.OnCameraIdleListener {

        locationManager?.visibleRegionBounds()?.let { bounds ->
            GeoHash.geoHashMatrix(bounds.northeast, bounds.southwest)?.forEach { geoHash ->

                firebase?.loadPokestops(geoHash)
                firebase?.loadArenas(geoHash)

            } ?: kotlin.run {
                Log.w(TAG, "Max zooming level reached!")
            }
        }

        clusterManager?.onCameraIdle()
    }

    private val onMarkerClickListener = GoogleMap.OnMarkerClickListener { marker ->
        clusterManager?.onMarkerClick(marker) ?: kotlin.run { false }
    }

    /**
     * FABs
     */

    // TODO: add button to show users registered geohash areas
    private fun setupFAB(fragmentLayout: View) {

        fragmentLayout.findViewById<FloatingActionsMenu>(R.id.fab_menu)?.setOnFloatingActionsMenuUpdateListener(object: FloatingActionsMenu.OnFloatingActionsMenuUpdateListener{
            override fun onMenuCollapsed() {
                resetMap()
            }

            override fun onMenuExpanded() {
                resetMap()
            }

            private fun resetMap() {
                dismissCrosshair()
                mapGridProvider?.clearGeoHashGridList()
                currentMode = Mode.DEFAULT
            }
        })

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_edit_geo_hashs)?.let { fab ->
            fab.setOnClickListener {

                dismissCrosshair()
                mapGridProvider?.clearGeoHashGridList()
                currentMode = Mode.EDIT_GEO_HASHES

                WaitingSpinner.showProgress()
                firebase?.loadSubscriptions { geoHashes ->
                    Log.i(TAG, "loading subscriptions for geoHashes: $geoHashes")
                    for (geoHash in geoHashes) {
                        mapGridProvider?.showGeoHashGrid(geoHash)
                    }
                    WaitingSpinner.hideProgress()
                }
            }
        }

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_arena)?.let { fab ->
            fab.setOnClickListener {

                showCrosshair()
                currentMode = Mode.NEW_ARENA
            }
        }

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_pokestop)?.let { fab ->
            fab.setOnClickListener {

                showCrosshair()
                currentMode = Mode.NEW_POKESTOP
            }
        }
    }

    /**
     * Permissions
     */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        context?.let {
            PermissionManager.onRequestPermissionsResult(requestCode, it, onLocationPermissionGranted = {
                locationManager?.focusStartLocation(activity)
            })
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

    /**
     * GPS
     */

    private fun showAlertMessageNoGPS() {

        context?.let { _context ->

            val builder = AlertDialog.Builder(_context)
            builder.setMessage(getString(R.string.dialog_gps_disabled_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_gps_disabled_button_positive)
                ) { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton(getString(R.string.dialog_gps_disabled_button_negative)
                ) { dialog, _ -> dialog.cancel() }

            builder.create().show()
        }
    }

    companion object {

        private val TAG = this::class.java.name

        const val GEO_HASH_AREA_PRECISION: Int = 6
    }
}