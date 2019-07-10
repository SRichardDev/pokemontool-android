package io.stanc.pogoradar.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions

object PermissionManager {

    private val TAG = javaClass.name

    private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    private const val REQUEST_CODE_LOCATION = 12349

    interface LocationPermissionObserver {
        fun onLocationPermissionGranted()
        fun onLocationPermissionDenied()
    }

    private val observerManager = ObserverManager<LocationPermissionObserver>()

    fun onRequestPermissionsResult(requestCode: Int, context: Context) {

        when(requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (isLocationPermissionGranted(context)) {
                    observerManager.observers().forEach { it?.onLocationPermissionGranted() }
                } else {
                    observerManager.observers().forEach { it?.onLocationPermissionDenied() }
                }
                observerManager.clear()
            }
        }
    }

    fun isLocationPermissionGranted(context: Context?): Boolean {

        context?.let {

            var allLocationPermissionsGranted = true

            LOCATION_PERMISSIONS.forEach {
                if (ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
                    allLocationPermissionsGranted = false
                }
            }
            return allLocationPermissionsGranted
        }

        return false
    }

    fun requestLocationPermissionIfNeeded(activity: Activity?, callback: LocationPermissionObserver? = null) {

        activity?.let {

            if (!isLocationPermissionGranted(activity)) {

                callback?.let { observerManager.addObserver(it) }

                requestPermissions(activity,
                    LOCATION_PERMISSIONS,
                    REQUEST_CODE_LOCATION
                )
            } else {
                callback?.onLocationPermissionGranted()
            }
        } ?: run {
            Log.e(TAG, "tried to request location permission, but activity is null!")
        }
    }
}