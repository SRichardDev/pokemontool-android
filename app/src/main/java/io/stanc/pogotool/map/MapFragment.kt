package io.stanc.pogotool.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.PermissionManager


open class MapFragment : Fragment() {

    private val TAG = javaClass.name

    var map: GoogleMap? = null

    private var mapView: MapView? = null
    private var delegate: MapDelegate? = null
    private var locationManager: LocationManager? = null

    private var geoHashStartPosition: GeoHash? = null
    private var isMyLocationEnabled: Boolean = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_map, container, false)

        AppbarManager.setTitle(getString(R.string.app_name))

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

        updateCameraStartPosition()
        map?.let { delegate?.onMapReady(it) }
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

    fun setNextStartPosition(latitude: Double, longitude: Double) {
        geoHashStartPosition = GeoHash(latitude, longitude)
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

    private fun updateCameraStartPosition() {

        if (PermissionManager.isLocationPermissionGranted(activity)) {

            geoHashStartPosition?.let {
                updateCameraPosition(it, onFinished = {
                    delegate?.onCameraStartAnimationFinished()
                })
            } ?: kotlin.run {
                latestLocation()?.let {
                    updateCameraPosition(it, onFinished = {
                        delegate?.onCameraStartAnimationFinished()
                    })
                }
            }

            geoHashStartPosition = null

        } else {
            PermissionManager.requestLocationPermission(activity)
        }
    }

    /**
     * camera position
     */

    fun updateCameraPosition(geoHash: GeoHash, onFinished: () -> Unit = {}) {
        val latlng = LatLng(geoHash.toLocation().latitude, geoHash.toLocation().longitude)
        updateCameraPosition(latlng, onFinished)
    }

    fun updateCameraPosition(location: Location, onFinished: () -> Unit = {}) {
        val latlng = LatLng(location.latitude, location.longitude)
        updateCameraPosition(latlng, onFinished)
    }

    fun updateCameraPosition(latLng: LatLng, onFinished: () -> Unit = {}) {

        map?.let { googleMap ->
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(ZOOM_LEVEL_STREET).build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animationCallback(onFinished))
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
        animationRunning = true
        animate(latLng)
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

    fun rotateCamera(latLng: LatLng, onFinished:() -> Unit = {}, durationInMilliSec: Int = defaultDurationInMilliSec) {

        map?.let { googleMap ->

            val lastBearing = googleMap.cameraPosition.bearing
            val bearing1 = lastBearing + 179
            val bearing2 = lastBearing - 2

            val camPosition1 = CameraPosition.builder()
                .target(latLng)
                .bearing(bearing1)
                .zoom(ZOOM_LEVEL_BUILDING)
                .tilt(googleMap.cameraPosition.tilt) // 30f
                .build()

            val camPosition2 = CameraPosition.builder()
                .target(latLng)
                .bearing(bearing2)
                .zoom(ZOOM_LEVEL_STREET)
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
                Log.i(TAG, "location permission granted! startAuthentication init camera animation...")
                updateCameraStartPosition()
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

        companion object {

        // google map zoom levels: https://developers.google.com/maps/documentation/android-sdk/views
        private const val ZOOM_LEVEL_WORLD: Float = 1.0f
        private const val ZOOM_LEVEL_LANDMASS: Float = 5.0f
        private const val ZOOM_LEVEL_CITY: Float = 10.0f
        private const val ZOOM_LEVEL_STREET: Float = 15.0f
        private const val ZOOM_LEVEL_BUILDING: Float = 20.0f

        private const val LOCATION_UPDATE_MIN_TIME_MILLISECONDS: Long = 1000
        private const val LOCATION_UPDATE_MIN_DISTANCE_METER: Float = 2.0f
    }
}