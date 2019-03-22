package io.stanc.pogotool

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.NOTIFICATION_DATA_LATITUDE
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.NOTIFICATION_DATA_LONGITUDE
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.ClusterManager
import io.stanc.pogotool.map.MapGeoHashGridProvider
import io.stanc.pogotool.utils.WaitingSpinner


class MapFragment: Fragment() {

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null

    // location
    private var locationManager: LocationManager? = null
    private var lastFocusedLocation: Location? = null
    private val locationListener = MapLocationListener()

    private var geoHashMapGridProvider: MapGeoHashGridProvider? = null
    private var geoHashStartPosition: GeoHash? = null

    private var crosshairImage: ImageView? = null
    private var crosshairAnimation: Animation? = null

    // firebase
    private val clusterManager = ClusterManager()
    private val firebase = FirebaseDatabase(clusterManager.pokestopDelegate, clusterManager.arenaDelegate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.intent?.extras?.let { bundle ->

            Log.d(TAG, "Debug:: Intent has extras [bundle.containsKey(\"$NOTIFICATION_DATA_LONGITUDE\"): ${bundle.containsKey(NOTIFICATION_DATA_LONGITUDE)}, bundle.containsKey(\"$NOTIFICATION_DATA_LATITUDE\"): ${bundle.containsKey(NOTIFICATION_DATA_LATITUDE)}]")

            if (bundle.containsKey(NOTIFICATION_DATA_LATITUDE) && bundle.containsKey(NOTIFICATION_DATA_LONGITUDE)) {

                val latitude = (bundle.get(NOTIFICATION_DATA_LATITUDE) as String).toDouble()
                val longitude = (bundle.get(NOTIFICATION_DATA_LONGITUDE) as String).toDouble()

                geoHashStartPosition = GeoHash(latitude, longitude)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentLayout = inflater.inflate(R.layout.layout_fragment_map, container, false)

        locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        mapView = fragmentLayout.findViewById(R.id.fragment_map_mapview) as MapView
        mapView?.onCreate(savedInstanceState)
        mapView?.onResume() // needed to get the map to display immediately

        activity?.let { MapsInitializer.initialize(it.applicationContext) }

        mapView?.getMapAsync { onMapReadyCallback(it) }

        // floating action buttons
        setupFAB(fragmentLayout)

        // crosshair
        crosshairAnimation = AnimationUtils.loadAnimation(context, R.anim.flashing)
        crosshairImage = fragmentLayout.findViewById(R.id.fragment_map_imageview_crosshair)

        return fragmentLayout
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        checkLocationPermission()
        checkGPSEnable()
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
     * Setup
     */

    private val onMapReadyCallback = { map: GoogleMap ->

        googleMap = map

        googleMap?.isBuildingsEnabled = true // 3D buildings

        setupOnMapClickListener()
        setupOnCameraIdleListener()

        // init view
        focusStartLocation()
        geoHashMapGridProvider = MapGeoHashGridProvider(map)
        geoHashMapGridProvider?.showGeoHashGridList()
    }

    private fun setupOnCameraIdleListener() {

        googleMap?.let { map ->

            context?.let { clusterManager.setup(it, map) }

            map.setOnMarkerClickListener { marker -> clusterManager.onMarkerClick(marker) }
            // TODO: needed?
//            map.setOnInfoWindowClickListener(pokestopClusterManager)

            map.setOnCameraIdleListener {
                googleMap?.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                    GeoHash.geoHashMatrix(bounds.northeast, bounds.southwest)?.forEach { geoHash ->

                        firebase.loadPokestops(geoHash)
                        firebase.loadArenas(geoHash)

                    } ?: kotlin.run {
                        Log.w(TAG, "Max zooming level reached!")
                    }
                }

                clusterManager.onCameraIdle()
            }
        }
    }

    enum class Mode {
        DEFAULT,
        EDIT_GEO_HASHES,
        NEW_ARENA,
        NEW_POKESTOP
    }
    private var currentMode = Mode.DEFAULT

    private fun setupOnMapClickListener() {

        googleMap?.setOnMapClickListener {

            when(currentMode) {

                Mode.NEW_ARENA -> {
                    FirebaseServer.currentUser?.name?.let { submitter ->
                        centeredPosition()?.let { latlng ->

                            // TODO: user should type name
                            val geoHash = GeoHash(latlng.latitude, latlng.longitude)
                            val arena = FirebaseArena("", "new Debug Arena", geoHash, submitter)
                            firebase.pushArena(arena)
                        }
                    }
                }

                Mode.NEW_POKESTOP -> {
                    FirebaseServer.currentUser?.name?.let { submitter ->
                        centeredPosition()?.let { latlng ->

                            // TODO: user should type name
                            val geoHash = GeoHash(latlng.latitude, latlng.longitude)
                            val pokestop = FirebasePokestop("", "new Debug Pokestop", geoHash, submitter)
                            firebase.pushPokestop(pokestop)
                        }
                    }
                }

                else -> {}
            }
        }

        googleMap?.setOnMapLongClickListener {

            if (isLocationPermissionGranted()) {

                if (currentMode == Mode.EDIT_GEO_HASHES) {

                    Log.d(TAG, "OnMapLongClickListener(Mode.EDIT_GEO_HASHES)")
                    firebase.formattedFirebaseGeoHash(GeoHash(it))?.let { geoHash ->
                        Log.d(TAG, "OnMapLongClickListener(Mode.EDIT_GEO_HASHES), clicked at $geoHash")

                        geoHashMapGridProvider?.let { mapProvider ->
                            if (mapProvider.geoHashExists(geoHash)) {

                                // TODO: add onCompletionCallBack for removing ...
                                Log.i(TAG, "Debug:: remove subscription and geoHashGrid for $geoHash...")
                                firebase.removeSubscription(geoHash)
                                mapProvider.removeGeoHashGrid(geoHash)

                            } else {

                                Log.i(TAG, "Debug:: add subscription and geoHashGrid for $geoHash...")
                                firebase.subscribeForPush(geoHash) { successful ->
                                    Log.i(TAG, "Debug:: subscribeForPush($geoHash), successful: $successful")
                                    mapProvider.showGeoHashGrid(geoHash)
                                }
                            }
                        }

                    }
                }

//                geoHashMapGridProvider?.toggleGeoHashGrid(it)
            } else {
                Toast.makeText(context, R.string.dialog_location_disabled_message, LENGTH_LONG).show()
            }
        }
    }

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
                geoHashMapGridProvider?.clearGeoHashGridList()
                currentMode = Mode.DEFAULT
            }
        })

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_edit_geo_hashs)?.let { fab ->
            fab.setOnClickListener {

                Log.i(TAG, "Debug:: tab fab_geo_hashs...")
                dismissCrosshair()
                geoHashMapGridProvider?.clearGeoHashGridList()
                currentMode = Mode.EDIT_GEO_HASHES

                WaitingSpinner.showProgress()
                firebase.loadSubscriptions { geoHashes ->
                    Log.i(TAG, "Debug:: geoHashes: $geoHashes")
                    for (geoHash in geoHashes) {
                        geoHashMapGridProvider?.showGeoHashGrid(geoHash)
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

    private fun checkLocationPermission() {
        if (!isLocationPermissionGranted()) {
            requestPermissions(
                LOCATION_PERMISSIONS,
                REQUEST_CODE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (isLocationPermissionGranted()) { focusStartLocation() }
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {

        context?.let { _context ->

            return  ActivityCompat.checkSelfPermission(_context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(_context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        return false
    }

    private fun requestLocationPermission() {

        requestPermissions(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_CODE_LOCATION
        )
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

    private fun centeredPosition(): LatLng? {
        return googleMap?.cameraPosition?.target
    }


    /**
     * Location
     */

    private fun focusStartLocation() {

        if (isLocationPermissionGranted()) {

            geoHashStartPosition?.let { focusLocation(it, shouldSetLocationMarker = true) }
                ?: kotlin.run { focusCurrentLocation() }

        } else {
            requestLocationPermission()
        }

        geoHashStartPosition = null
    }

    private fun focusLocation(geoHash: GeoHash, shouldSetLocationMarker: Boolean = false) {
        focusLocation(geoHash.toLocation(), shouldSetLocationMarker)
    }

    @SuppressLint("MissingPermission")
    private fun focusLocation(location: Location, shouldSetLocationMarker: Boolean = false) {

        googleMap?.let { map ->

            // For showing a move to my location button
            map.isMyLocationEnabled = true

            locationManager?.let { manager ->

                // Use the location manager through GPS
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME_MILLISECONDS,
                    LOCATION_UPDATE_MIN_DISTANCE_METER, locationListener)

                updateCameraPosition(location)
                lastFocusedLocation = location

                //when the current location is found – stop listening for updates (preserves battery)
                manager.removeUpdates(locationListener)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun focusCurrentLocation() {
        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { lastLocation -> focusLocation(lastLocation) }
    }

    private fun updateCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        updateCameraPosition(latlng)
    }

    private fun updateCameraPosition(latLng: LatLng) {

        googleMap?.let { _googleMap ->

            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(ZOOM_LEVEL_STREET).build()
            _googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private inner class MapLocationListener: LocationListener {

        override fun onLocationChanged(location: Location?) {
            Log.d(this::class.java.name, "Debug:: onLocationChanged(latitude: ${location?.latitude}, longitude: ${location?.longitude})")
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(this::class.java.name, "Debug:: onStatusChanged(provider: $provider, status: $status)")
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onProviderEnabled(provider: String?) {
            Log.d(this::class.java.name, "Debug:: onProviderEnabled(provider: $provider)")
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onProviderDisabled(provider: String?) {
            Log.d(this::class.java.name, "Debug:: onProviderDisabled(provider: $provider)")
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    /**
     * GPS
     */

    private fun checkGPSEnable() {

        locationManager?.let {

            if (!it.isProviderEnabled( LocationManager.GPS_PROVIDER )) {
                showAlertMessageNoGPS()
            }
        }
    }

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

    /**
     * Firebase
     */

    // TODO... -> FirebaseServer.kt
    private fun subscribeForGeoHashes(context: Context) {

        var allTasksSuccessful = true
        geoHashMapGridProvider?.geoHashes()?.forEach { firebase.subscribeForPush(it) { taskSuccessful: Boolean ->
                if (!taskSuccessful) { allTasksSuccessful = false }
            }
        }

        if (allTasksSuccessful) {
            Toast.makeText(context, context.getString(R.string.locations_sent_sucessfully), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, context.getString(R.string.locations_not_sent_sucessfully), Toast.LENGTH_LONG).show()
        }
    }

    companion object {

        private val TAG = this::class.java.name

        private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        private const val REQUEST_CODE_LOCATION = 12349

        private const val LOCATION_UPDATE_MIN_TIME_MILLISECONDS: Long = 1000
        private const val LOCATION_UPDATE_MIN_DISTANCE_METER: Float = 2.0f

        // google map zoom levels: https://developers.google.com/maps/documentation/android-sdk/views
        private const val ZOOM_LEVEL_WORLD: Float = 1.0f
        private const val ZOOM_LEVEL_LANDMASS: Float = 5.0f
        private const val ZOOM_LEVEL_CITY: Float = 10.0f
        private const val ZOOM_LEVEL_STREET: Float = 15.0f
        private const val ZOOM_LEVEL_BUILDING: Float = 20.0f

        const val GEO_HASH_AREA_PRECISION: Int = 6
    }
}