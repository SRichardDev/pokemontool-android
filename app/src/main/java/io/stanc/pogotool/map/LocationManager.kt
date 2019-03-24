package io.stanc.pogotool.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.PermissionManager

class LocationManager(private val googleMap: GoogleMap,
                      context: Context) {
    
    init {
        googleMap.isBuildingsEnabled = true // 3D buildings
    }

    private var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val locationListener = MapLocationListener()

    private var lastFocusedLocation: Location? = null
    private var geoHashStartPosition: GeoHash? = null

    /**
     * on map click listener
     */

    fun setOnMapClickListener(onMapClickListener: GoogleMap.OnMapClickListener) {
        googleMap.setOnMapClickListener(onMapClickListener)
    }

    fun setOnMapLongClickListener(onMapLongClickListener: GoogleMap.OnMapLongClickListener) {
        googleMap.setOnMapLongClickListener(onMapLongClickListener)
    }

    fun setOnCameraIdleListener(onCameraIdleListener: GoogleMap.OnCameraIdleListener) {
        googleMap.setOnCameraIdleListener(onCameraIdleListener)
    }

    fun setOnMarkerClickListener(onMarkerClickListener: GoogleMap.OnMarkerClickListener) {
        googleMap.setOnMarkerClickListener(onMarkerClickListener)
    }

    /**
     * focus locations
     */

    fun setNextStartLocation(latitude: Double, longitude: Double) {
        geoHashStartPosition = GeoHash(latitude, longitude)
    }

    fun focusStartLocation(activity: Activity?) {

        if (PermissionManager.isLocationPermissionGranted(activity)) {

            geoHashStartPosition?.let { focusLocation(it) }
                ?: kotlin.run { focusLatestLocation() }

            geoHashStartPosition = null

        } else {
            PermissionManager.requestLocationPermission(activity)
        }
    }

    fun focusLocation(geoHash: GeoHash) {
        focusLocation(geoHash.toLocation())
    }

    @SuppressLint("MissingPermission")
    fun focusLocation(location: Location) {

        googleMap.let { map ->

            // For showing a move to my location button
            map.isMyLocationEnabled = true

            locationManager?.let { manager ->

                // Use the location manager through GPS
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME_MILLISECONDS,
                    LOCATION_UPDATE_MIN_DISTANCE_METER, locationListener)

                updateCameraPosition(location)
                lastFocusedLocation = location

                // TODO: does this work?
                //when the current location is found – stop listening for updates (preserves battery)
                manager.removeUpdates(locationListener)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun focusLatestLocation() {
        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { lastLocation -> focusLocation(lastLocation) }
    }

    /**
     * camera
     */

    fun updateCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        updateCameraPosition(latlng)
    }

    fun updateCameraPosition(latLng: LatLng) {

        googleMap.let {

            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(ZOOM_LEVEL_STREET).build()
            it.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    /**
     * helper
     */

    fun centeredPosition(): LatLng? {
        return googleMap.cameraPosition?.target
    }

    fun isGPSEnabled(): Boolean {
        return locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER )
    }

    fun visibleRegionBounds(): LatLngBounds? {
        return googleMap.projection?.visibleRegion?.latLngBounds
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

    companion object {

        private const val LOCATION_UPDATE_MIN_TIME_MILLISECONDS: Long = 1000
        private const val LOCATION_UPDATE_MIN_DISTANCE_METER: Float = 2.0f

        // google map zoom levels: https://developers.google.com/maps/documentation/android-sdk/views
        private const val ZOOM_LEVEL_WORLD: Float = 1.0f
        private const val ZOOM_LEVEL_LANDMASS: Float = 5.0f
        private const val ZOOM_LEVEL_CITY: Float = 10.0f
        private const val ZOOM_LEVEL_STREET: Float = 15.0f
        private const val ZOOM_LEVEL_BUILDING: Float = 20.0f
    }
}