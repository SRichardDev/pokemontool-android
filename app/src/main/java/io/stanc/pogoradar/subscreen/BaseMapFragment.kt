package io.stanc.pogoradar.subscreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import io.stanc.pogoradar.R
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.subscreen.ZoomLevel.*
import io.stanc.pogoradar.utils.PermissionManager
import android.content.Context.LOCATION_SERVICE
import android.widget.Toast
import io.stanc.pogoradar.App
import com.google.android.gms.maps.model.MapStyleOptions
import android.content.res.Resources


// google map zoom levels: https://developers.google.com/maps/documentation/android-sdk/views
enum class ZoomLevel(val value: Float) {
    WORLD(1.0f),
    LANDMASS(5.0f),
    CITY(10.0f),
    DISTRICT(15.0f),
    NEIGHBORHOOD(17.0f),
    STREET(18.5f),
    BUILDING(20.0f)
}

open class BaseMapFragment : Fragment() {

    private val TAG = javaClass.name

    var map: GoogleMap? = null
    var mapView: MapView? = null
        private set
    private var delegate: MapDelegate? = null
    private var locationManager: LocationManager? = null

    private var geoHashStartPosition: GeoHash? = null
    private var zoomLevelStart: ZoomLevel? = null
    private val zoomLevelDefault: ZoomLevel = DISTRICT
    private var isMyLocationEnabled: Boolean = true

    private val locationPermissionCallback = object : PermissionManager.LocationPermissionObserver {

        override fun onLocationPermissionGranted() {
            updateMyLocationEnabledPOI()
            moveCameraToStartPosition()
        }

        override fun onLocationPermissionDenied() {
            Toast.makeText(context, App.geString(R.string.exceptions_location_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = rootLayout.findViewById(R.id.map_mapview) as MapView
        mapView?.onCreate(savedInstanceState)

        activity?.let { MapsInitializer.initialize(it.applicationContext) }

        mapView?.getMapAsync { setupMap(it) }

        return rootLayout
    }

    private fun setupMap(googleMap: GoogleMap) {
        map = googleMap

        setupCustomMapStyle(googleMap)

        // 3D buildings
        map?.isBuildingsEnabled = true

        updateMyLocationEnabledPOI()
        moveCameraToStartPosition()

        map?.let { delegate?.onMapReady(it) }
    }

    private fun setupCustomMapStyle(googleMap: GoogleMap) {

        // https://stackoverflow.com/questions/22660778/is-it-possible-to-remove-the-default-points-of-interest-from-android-google-map
        try {
            val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
            val success = googleMap.setMapStyle(mapStyleOptions)

            if (!success) {
                Log.e(TAG, "Google map style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find map style. NotFoundException: ", e)
        }
    }

    /**
     * mapView lifecycle
     */

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()

        if (!PermissionManager.isLocationPermissionGranted(context)) {
            PermissionManager.requestLocationPermissionIfNeeded(activity, locationPermissionCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
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
     * map positions
     */

    fun addMarker(markerOptions: MarkerOptions): Marker? {
        return map?.addMarker(markerOptions)
    }

    fun centeredPosition(): LatLng? {
        return map?.cameraPosition?.target
    }

    fun visibleRegionBounds(): LatLngBounds? {
        return map?.projection?.visibleRegion?.latLngBounds
    }
    fun enableMyLocationPOI(enabled: Boolean) {
        isMyLocationEnabled = enabled
        updateMyLocationEnabledPOI()
    }

    @SuppressLint("MissingPermission") // permission check exists
    // for showing myPOI and button to my location
    private fun updateMyLocationEnabledPOI() {

        if (PermissionManager.isLocationPermissionGranted(context)) {
            map?.isMyLocationEnabled = isMyLocationEnabled
            map?.uiSettings?.isMyLocationButtonEnabled = isMyLocationEnabled
            map?.setOnMyLocationButtonClickListener {

                if (!isGPSEnabled()) {
                    showAlertMessageNoGPS()
                } else {
                    moveToMyLocation()
                }
                true
            }
        }
    }

    private fun moveCameraToStartPosition() {

        map?.let { map ->

            val zoomLevel: ZoomLevel = zoomLevelStart ?: zoomLevelDefault

            geoHashStartPosition?.let {

                val cameraPosition = CameraPosition.Builder()
                    .target(it.toLatLng()).zoom(zoomLevel.value).build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                delegate?.onCameraStartAnimationFinished()

//                val startPosition = CameraUpdateFactory.newLatLng(it.toLatLng())
//                map.moveCamera(startPosition)
//                delegate?.onCameraStartAnimationFinished()

            } ?: run {
                lastKnownLocation()?.let {

//                    val startPosition = CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))
//                    map.moveCamera(startPosition)
//                    delegate?.onCameraStartAnimationFinished()

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(it.latitude, it.longitude)).zoom(zoomLevel.value).build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    delegate?.onCameraStartAnimationFinished()
                }
            }

            geoHashStartPosition = null
            zoomLevelStart = null

        } ?: run {
            Log.w(TAG, "moveCameraToStartPosition not working, map is not ready!")
        }
    }

    /**
     * camera position
     */

    private fun moveToMyLocation() {
        lastKnownLocation()?.let { location ->
            updateCameraPosition(LatLng(location.latitude, location.longitude), NEIGHBORHOOD)
        }
    }

    fun updateCameraPosition(geoHash: GeoHash, zoomLevel: ZoomLevel = zoomLevelDefault, onFinished: () -> Unit = {}) {
        val latlng = LatLng(geoHash.toLocation().latitude, geoHash.toLocation().longitude)
        updateCameraPosition(latlng, zoomLevel, onFinished)
    }

    fun updateCameraPosition(location: Location, zoomLevel: ZoomLevel = zoomLevelDefault, onFinished: () -> Unit = {}) {
        val latlng = LatLng(location.latitude, location.longitude)
        updateCameraPosition(latlng, zoomLevel, onFinished)
    }

    fun updateCameraPosition(latLng: LatLng, zoomLevel: ZoomLevel = zoomLevelDefault, onFinished: () -> Unit = {}) {
        Log.d(TAG, "Debug:: updateCameraPosition($latLng, ${zoomLevel.name}), map: $map")

        map?.let { googleMap ->
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(zoomLevel.value).build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animationCallback(onFinished))
        } ?: run {
            geoHashStartPosition = GeoHash(latLng)
            Log.w(TAG, "Debug:: updateCameraPosition($latLng), map not ready, store geoHashStartPosition: $geoHashStartPosition")
        }
    }

    fun zoomTo(targetZoomLevel: ZoomLevel, smoothZooming: Boolean = false) {
        if (smoothZooming) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(centeredPosition(), targetZoomLevel.value), 750, null)
        } else {
            map?.moveCamera(CameraUpdateFactory.zoomTo(targetZoomLevel.value))
        }
    }


    private fun animationCallback(onFinished: () -> Unit): GoogleMap.CancelableCallback {

        return object: GoogleMap.CancelableCallback {
            override fun onFinish() {
                onFinished()
            }

            override fun onCancel() {
                Log.w(TAG, "animation canceled.")
            }
        }
    }

    /**
     * camera animation
     */

    private var animationRunning = false
    private val defaultDurationInMilliSec = 6000

    fun startAnimation(latLng: LatLng) {
        if (!animationRunning) {
            animate(latLng)
            animationRunning = true
        }
    }

    fun stopAnimation() {
        animationRunning = false
    }

    private fun animate(latLng: LatLng) {
        rotateCamera(latLng, onFinished = {
            if (animationRunning) {
                animate(latLng)
            }
        })
    }

    private fun rotateCamera(latLng: LatLng, onFinished:() -> Unit = {}, durationInMilliSec: Int = defaultDurationInMilliSec) {

        map?.let { googleMap ->

            val lastBearing = googleMap.cameraPosition.bearing
            val bearing1 = lastBearing + 179
            val bearing2 = lastBearing - 2

            val camPosition1 = CameraPosition.builder()
                .target(latLng)
                .bearing(bearing1)
                .zoom(STREET.value)
                .tilt(googleMap.cameraPosition.tilt) // 30f
                .build()

            val camPosition2 = CameraPosition.builder()
                .target(latLng)
                .bearing(bearing2)
                .zoom(DISTRICT.value)
                .tilt(75f)
                .build()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition1),
                                    durationInMilliSec,
                                    animationCallback(onFinished = {
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPosition2),
                                        durationInMilliSec,
                                        animationCallback(onFinished))
            }))
        }
    }

    fun rotateCameraAroundCenter() {
        centeredPosition()?.let { rotateCamera(it) }
    }

    /**
     * Location
     */

    private fun lastKnownLocation(): Location? {

        var location: Location? = null

        try {

            val gpsIsEnabled = isGPSEnabled()
            val networkIsEnabled = isNetworkEnabled()

            if (!gpsIsEnabled && !networkIsEnabled) {
                Log.w(TAG, "GPS and Network is disabled! No location found.")
            }

            if (gpsIsEnabled) {
                location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                Log.v(TAG, "lastKnownLocation(${LocationManager.GPS_PROVIDER}): $location")
            }

            if (networkIsEnabled && location == null) {
                location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                Log.v(TAG, "lastKnownLocation(${LocationManager.NETWORK_PROVIDER}): $location")
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException => No location found.")
            e.printStackTrace()
        }

        return location
    }


    private fun isGPSEnabled(): Boolean = locationManager?.isProviderEnabled( LocationManager.GPS_PROVIDER ) ?: false
    private fun isNetworkEnabled(): Boolean = locationManager?.isProviderEnabled( LocationManager.NETWORK_PROVIDER ) ?: false

    private fun showAlertMessageNoGPS() {

        context?.let { _context ->

            val builder = AlertDialog.Builder(_context)
                .setMessage(getString(R.string.dialog_gps_disabled_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_gps_disabled_button_positive)) { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(getString(R.string.dialog_gps_disabled_button_negative)) { dialog, _ ->
                    dialog.cancel()
                }

            builder.create().show()
        }
    }

    /**
     * observer
     */

    interface MapDelegate {
        fun onMapReady(googleMap: GoogleMap)
        fun onCameraStartAnimationFinished()
    }

    fun setDelegate(delegate: MapDelegate) {
        map?.let { delegate.onMapReady(it) }
        this.delegate = delegate
    }
}