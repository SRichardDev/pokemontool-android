package io.stanc.pogotool.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.requestPermissions
import android.util.Log

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
            return  ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        return false
    }

    fun requestLocationPermission(activity: Activity?) {

        activity?.let {

            requestPermissions(it,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        } ?: kotlin.run {
            Log.e(TAG, "tried to request location permission, but activity is null!")
        }
    }
}