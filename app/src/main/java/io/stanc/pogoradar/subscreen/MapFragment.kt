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
import io.stanc.pogoradar.utils.PermissionManager
import io.stanc.pogoradar.subscreen.ZoomLevel.BUILDING
import io.stanc.pogoradar.subscreen.ZoomLevel.STREETS
import io.stanc.pogoradar.subscreen.ZoomLevel.STREET
import com.google.android.gms.maps.model.CameraPosition

// google map zoom levels: https://developers.google.com/maps/documentation/android-sdk/views
enum class ZoomLevel(val value: Float) {
    WORLD(1.0f),
    LANDMASS(5.0f),
    CITY(10.0f),
    STREETS(15.0f),
    STREET(18.0f),
    BUILDING(20.0f)
}

open class MapFragment : Fragment() {

    private val TAG = javaClass.name

    var map: GoogleMap? = null
    var mapView: MapView? = null
        private set
    private var delegate: MapDelegate? = null
    private var locationManager: LocationManager? = null

    private var geoHashStartPosition: GeoHash? = null
    private var zoomLevelStart: ZoomLevel? = null
    private val zoomLevelDefault: ZoomLevel = STREETS
    private var isMyLocationEnabled: Boolean = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = rootLayout.findViewById(R.id.map_mapview) as MapView
        mapView?.onCreate(savedInstanceState)
        mapView?.onResume() // needed to get the map to display immediately

        activity?.let { MapsInitializer.initialize(it.applicationContext) }

        mapView?.getMapAsync { setupMap(it) }

        return rootLayout
    }

    private fun setupMap(googleMap: GoogleMap) {
        map = googleMap

        // 3D buildings
        map?.isBuildingsEnabled = true

        updateMyLocationEnabledPOI()
        Log.v(TAG, "Debug:: setupMap()")
        moveCameraToStartPosition()

        map?.let { delegate?.onMapReady(it) }

        if (!PermissionManager.isLocationPermissionGranted(activity)) {
            PermissionManager.requestLocationPermission(activity)
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
        activity?.let { PermissionManager.checkLocationPermission(it) }
        isGPSEnabled()?.let { enabled ->
            if (!enabled) {
                showAlertMessageNoGPS()
            }
        }
        updateMyLocationEnabledPOI()
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
     * Location
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

    @SuppressLint("MissingPermission") // permission check exists
    fun latestLocation(): Location? {
        return if (PermissionManager.isLocationPermissionGranted(context)) {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else {
            null
        }
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
        }
    }

    private fun moveCameraToStartPosition() {
        Log.d(TAG, "Debug:: moveCameraToStartPosition(), geoHashStartPosition: $geoHashStartPosition, map: $map")

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
                latestLocation()?.let {

//                    val startPosition = CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))
//                    map.moveCamera(startPosition)
//                    delegate?.onCameraStartAnimationFinished()

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(it.latitude, it.longitude)).zoom(zoomLevel.value).build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    delegate?.onCameraStartAnimationFinished()
                }
            }

            Log.d(TAG, "Debug:: reset geoHashStartPosition")
            geoHashStartPosition = null
            zoomLevelStart = null

        } ?: run {
            Log.w(TAG, "moveCameraToStartPosition not working, map is not ready!")
        }
    }

    /**
     * camera position
     */

    fun updateCameraPosition(geoHash: GeoHash, zoomLevel: ZoomLevel = zoomLevelDefault, onFinished: () -> Unit = {}) {
        val latlng = LatLng(geoHash.toLocation().latitude, geoHash.toLocation().longitude)
        updateCameraPosition(latlng, zoomLevel, onFinished)
    }

    fun updateCameraPosition(location: Location, zoomLevel: ZoomLevel = zoomLevelDefault, onFinished: () -> Unit = {}) {
        val latlng = LatLng(location.latitude, location.longitude)
        updateCameraPosition(latlng, zoomLevel, onFinished)
    }

    fun updateCameraPosition(latLng: LatLng, zoomLevel: ZoomLevel = zoomLevelDefault, onFinished: () -> Unit = {}) {
        Log.d(TAG, "Debug:: updateCameraPosition($latLng, ${zoomLevel.name})")

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
                .zoom(BUILDING.value)
                .tilt(googleMap.cameraPosition.tilt) // 30f
                .build()

            val camPosition2 = CameraPosition.builder()
                .target(latLng)
                .bearing(bearing2)
                .zoom(STREET.value)
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
     * Permissions
     */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        context?.let {
            PermissionManager.onRequestPermissionsResult(requestCode, it, onLocationPermissionGranted = {
                Log.v(TAG, "Debug:: onRequestPermissionsResult()")
                updateMyLocationEnabledPOI()
                moveCameraToStartPosition()
            })
        }
    }

    /**
     * GPS
     */

    private fun isGPSEnabled(): Boolean? = locationManager?.isProviderEnabled( LocationManager.GPS_PROVIDER )

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