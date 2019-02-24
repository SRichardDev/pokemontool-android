package io.stanc.pogotool

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.annotation.DrawableRes
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
import com.google.android.gms.maps.model.*
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.firebase.FirebaseServer.NOTIFICATION_DATA_LATITUDE
import io.stanc.pogotool.firebase.FirebaseServer.NOTIFICATION_DATA_LONGITUDE
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.firebase.data.FirebaseSubscription
import io.stanc.pogotool.geohash.GeoHash


class MapFragment: Fragment() {

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null

    // location
    private var locationManager: LocationManager? = null
    private var lastFocusedLocation: Location? = null
    private val locationListener = MapLocationListener()

    private var geoHashStartPosition: GeoHash? = null
    private val geoHashList: HashMap<GeoHash, Polygon> = HashMap()

    private var crosshairImage: ImageView? = null
    private var crosshairAnimation: Animation? = null

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

        mapView?.getMapAsync {onMapReadyCallback(it)}

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

    private val onMapReadyCallback = { it: GoogleMap ->

        googleMap = it
        googleMap?.isBuildingsEnabled = true // 3D buildings

        googleMap?.setOnMapLongClickListener {

            if (isLocationPermissionGranted()) {
                toggleGeoHashGrid(it)
            } else {
                Toast.makeText(context, R.string.dialog_location_disabled_message, LENGTH_LONG).show()
            }
        }

        // on camera move finished
        googleMap?.setOnCameraIdleListener {
            Log.d(TAG, "Debug:: camera move finished.")
//            update arenas and pokestops for this camera view
            googleMap?.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                removeAllMarkers()
                FirebaseServer.requestForData(bounds.northeast, bounds.southwest,onNewArenaCallback = {
                    setLocationMarker(it.geoHash.toLocation(), MarkerType.arena)
                }, onNewPokestopCallback = {
                    setLocationMarker(it.geoHash.toLocation(), MarkerType.pokestop)
                })
            }
        }

        focusStartLocation()

        showGeoHashGridList()
    }

    private fun setupFAB(fragmentLayout: View) {

        fragmentLayout.findViewById<FloatingActionsMenu>(R.id.fab_menu)?.setOnFloatingActionsMenuUpdateListener(object: FloatingActionsMenu.OnFloatingActionsMenuUpdateListener{
            override fun onMenuCollapsed() {
                dismissCrosshair()
            }

            override fun onMenuExpanded() { /* is not needed */ }
        })

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_geo_hash)?.let { fab ->
            fab.setOnClickListener {
                context?.let { updateData(it) }
            }
        }

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_arena)?.let { fab ->
            fab.setOnClickListener {
                // TODO:
                // 1. send arena -> Firebase
                // 2. register for events
                // 3. get new arena from firebase -> show marker
                centeredPosition()?.let {
                    setLocationMarker(it, MarkerType.arena)
                    FirebaseServer.sendArena("new Debug Arena",
                        GeoHash(it.latitude, it.longitude)
                    )
                }

            }
        }

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_pokestop)?.let { fab ->
            fab.setOnClickListener {
                // TODO:
                // 1. send pokestop -> Firebase
                // 2. register for events
                // 3. get new pokestop from firebase -> show marker
                centeredPosition()?.let {
                    setLocationMarker(it, MarkerType.pokestop)
                    FirebaseServer.sendPokestop("new Debug Pokestop",
                        GeoHash(it.latitude, it.longitude)
                    )
                }
            }
        }

        fragmentLayout.findViewById<FloatingActionButton>(R.id.fab_crosshair)?.let { fab ->
            fab.setOnClickListener {
                context?.let { showCrosshair() }
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
     * GeoHash Grid
     */

    private fun toggleGeoHashGrid(latlng: LatLng) {
        val geoHash = GeoHash(
            latlng.latitude,
            latlng.longitude,
            GEO_HASH_AREA_PRECISION
        )
        toggleGeoHashGrid(geoHash)
    }

    private fun toggleGeoHashGrid(geoHash: GeoHash) {

        if (geoHashList.contains(geoHash)) {
            geoHashList.remove(geoHash)?.remove()
        } else {
            showGeoHashGrid(geoHash)
        }
    }

    private fun showGeoHashGridList() {
        geoHashList.keys.forEach { showGeoHashGrid(it) }
    }

    private fun showGeoHashGrid(location: Location) {
        val geoHash =
            GeoHash(location, GEO_HASH_AREA_PRECISION)
        showGeoHashGrid(geoHash)
    }

    private fun showGeoHashGrid(latlng: LatLng) {
        val geoHash = GeoHash(
            latlng.latitude,
            latlng.longitude,
            GEO_HASH_AREA_PRECISION
        )
        showGeoHashGrid(geoHash)
    }

    private fun showGeoHashGrid(geoHash: GeoHash) {

        if (geoHashList.contains(geoHash)) {
            geoHashList[geoHash]?.remove()
        }

        val polygonRect = PolygonOptions()
            .strokeWidth(5.0f)
            .fillColor(R.color.colorGeoHashArea)
            .add(
                LatLng(geoHash.boundingBox.bottomLeft.latitude, geoHash.boundingBox.bottomLeft.longitude),
                LatLng(geoHash.boundingBox.topLeft.latitude, geoHash.boundingBox.topLeft.longitude),
                LatLng(geoHash.boundingBox.topRight.latitude, geoHash.boundingBox.topRight.longitude),
                LatLng(geoHash.boundingBox.bottomRight.latitude, geoHash.boundingBox.bottomRight.longitude),
                LatLng(geoHash.boundingBox.bottomLeft.latitude, geoHash.boundingBox.bottomLeft.longitude)
            )

        googleMap?.addPolygon(polygonRect)?.let { geoHashList[geoHash] = it }
    }

    /**
     * Location
     */

    enum class MarkerType {
        default,
        arena,
        pokestop
    }

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
                if (shouldSetLocationMarker) {
                    setLocationMarker(location)
                }
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

    private fun setLocationMarker(location: Location, markerType: MarkerType = MarkerType.default) {
        val latlng = LatLng(location.latitude, location.longitude)
        setLocationMarker(latlng, markerType)
    }

    private fun setLocationMarker(latLng: LatLng, markerType: MarkerType = MarkerType.default) {

        val markerOptions = MarkerOptions().position(latLng)
//            .title(getString(R.string.location_marker_current_position_title))
//            .snippet(getString(R.string.location_marker_current_position_description))

        when(markerType) {

            MarkerType.default -> {}
            MarkerType.arena -> markerOptions.icon(getBitmapDescriptor(R.drawable.icon_arenaex_30dp))
            MarkerType.pokestop -> markerOptions.icon(getBitmapDescriptor(R.drawable.icon_pstop_30dp))
        }

        googleMap?.addMarker(markerOptions)
    }

    private fun removeAllMarkers() {
        googleMap?.clear()
    }

    private fun getBitmapDescriptor(@DrawableRes id: Int): BitmapDescriptor {
        val vectorDrawable = context?.getDrawable(id)
//        val h = ((int) Utils?.convertDpToPixel(42, context));
//        val w = ((int) Utils?.convertDpToPixel(25, context));
        vectorDrawable?.setBounds(0, 0, 50, 50)
        val bm = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable?.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
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
    private fun updateData(context: Context) {

        var allTasksSuccessful = true
        geoHashList.keys.forEach { FirebaseServer.subscribeForPush(it) { taskSuccessful: Boolean ->
            if (!taskSuccessful) { allTasksSuccessful = false }
        } }

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