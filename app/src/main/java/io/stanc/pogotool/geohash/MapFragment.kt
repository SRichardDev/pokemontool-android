package io.stanc.pogotool.geohash

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
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseServer


class MapFragment: Fragment() {

    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null

    // location
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var currentLocationMarker: Marker? = null
    private val locationListener = MapLocationListener()

    private val geoHashList: HashMap<GeoHash, Polygon> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(this::class.java.name, "Debug:: onCreate()")
        if (!isLocationPermissionGranted()) {
            requestPermissions(
                LOCATION_PERMISSIONS,
                REQUEST_CODE_LOCATION
            )
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

        return fragmentLayout
    }

    private val onMapReadyCallback = { it: GoogleMap ->

        googleMap = it

        googleMap?.setOnMapLongClickListener {

            if (isLocationPermissionGranted()) {
                toggleGeoHashGrid(it)
            } else {
                Toast.makeText(context, R.string.dialog_location_disabled_message, LENGTH_LONG).show()
            }
        }

        if (isLocationPermissionGranted()) {
            showCurrentLocation()
        }

        showGeoHashGridList()
    }

    override fun onResume() {
        super.onResume()
        Log.d(this::class.java.name, "Debug:: onResume()")
        mapView?.onResume()
        checkGPS()
    }

    override fun onPause() {
        super.onPause()
        Log.d(this::class.java.name, "Debug:: onPause()")
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(this::class.java.name, "Debug:: onDestroy()")
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(this::class.java.name, "Debug:: onLowMemory()")
        mapView?.onLowMemory()
    }

    /**
     * Permissions
     */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(MapFragment::class.java.name, "Debug:: onRequestPermissionsResult(requestCode: $requestCode)")

        when(requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (isLocationPermissionGranted()) {
                    Log.d(MapFragment::class.java.name, "Debug:: onRequestPermissionsResult(), isLocationPermissionGranted: true")
                    showCurrentLocation()
                }
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
     * Location
     */

    private fun toggleGeoHashGrid(latlng: LatLng) {
        val geoHash = GeoHash(latlng.latitude, latlng.longitude, GEO_HASH_PRECISION)
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
        val geoHash = GeoHash(location, GEO_HASH_PRECISION)
        showGeoHashGrid(geoHash)
    }

    private fun showGeoHashGrid(latlng: LatLng) {
        val geoHash = GeoHash(latlng.latitude, latlng.longitude, GEO_HASH_PRECISION)
        showGeoHashGrid(geoHash)
    }

    private fun showGeoHashGrid(geoHash: GeoHash) {
        Log.d(this::class.java.name, "Debug:: showGeoHashGrid: geoHash: $geoHash, location: ${geoHash.toLocation()}")

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

    fun updateCurrentLocation() {
        if (isLocationPermissionGranted()) {
            showCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showCurrentLocation() {
        Log.d(this::class.java.name, "Debug:: showCurrentLocation(), googleMap: $googleMap, locationManager: $locationManager")

        googleMap?.let { _map ->

            // For showing a move to my location button
            _map.isMyLocationEnabled = true

            locationManager?.let { _locationManager ->

                Log.d(this::class.java.name, "Debug:: showCurrentLocation() -> requestLocationUpdates")

                // Use the location manager through GPS
                _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME_MILLISECONDS,
                    LOCATION_UPDATE_MIN_DISTANCE_METER, locationListener)

                //get the current location (last known location) from the location manager
                lastLocation = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lastLocation?.let {
                    updateLocationMarker(it)
                    updateCameraPosition(it)
                }

                Log.d(this::class.java.name, "Debug:: showCurrentLocation() -> removeUpdates")
                //when the current location is found – stop listening for updates (preserves battery)
                _locationManager.removeUpdates(locationListener)
            }
        }
    }

    private fun updateLocationMarker(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        updateLocationMarker(latlng)
    }

    private fun updateLocationMarker(latLng: LatLng) {

        googleMap?.let { _googleMap ->

            currentLocationMarker = _googleMap.addMarker(MarkerOptions().position(latLng)
                .title(getString(R.string.location_marker_current_position_title))
                .snippet(getString(R.string.location_marker_current_position_description)))
        }
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

    private fun checkGPS() {

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
    fun updateData() {
        // firebase....

        FirebaseServer.updateUserName("testName")
    }


    companion object {

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

        private const val GEO_HASH_PRECISION: Int = 6

    }
}