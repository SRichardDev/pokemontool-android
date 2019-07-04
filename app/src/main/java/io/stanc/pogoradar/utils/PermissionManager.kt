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

    fun checkLocationPermission(activity: Activity?) {

        if (!isLocationPermissionGranted(activity)) {
            requestLocationPermission(activity)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, context: Context, onLocationPermissionGranted: () -> Unit) {

        when(requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (isLocationPermissionGranted(context)) { onLocationPermissionGranted() }
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

    fun requestLocationPermission(activity: Activity?) {

        activity?.let {

            requestPermissions(it,
                LOCATION_PERMISSIONS,
                REQUEST_CODE_LOCATION
            )
        } ?: run {
            Log.e(TAG, "tried to request location permission, but activity is null!")
        }
    }
}