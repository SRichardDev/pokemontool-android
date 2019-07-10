package io.stanc.pogoradar.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import java.lang.ref.WeakReference


object MapLocationManager {

//    var mLocationRequest: LocationRequest? = null
//    var mLastLocation: Location? = null
//    private var mFusedLocationClient: FusedLocationProviderClient? = null
//    private var context: WeakReference<Context>? = null
//
//    fun setup(context: Context) {
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
//    }
//
//    @SuppressLint("MissingPermission")
//    fun onMapReady(googleMap: GoogleMap) {
//
//        mLocationRequest = LocationRequest()
//        mLocationRequest?.interval = 120000 // two minute interval
//        mLocationRequest?.fastestInterval = 120000
//        mLocationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//            if(PermissionManager.isLocationPermissionGranted(context?.get())) {
//                mFusedLocationClient?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
//                googleMap.isMyLocationEnabled = true
//            } else {
//                //Request Location Permission
//                checkLocationPermission()
//            }
//        } else {
//            mFusedLocationClient?.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
//            googleMap.isMyLocationEnabled = true
//        }
//    }

//    private fun checkLocationPermission() {
//        // Should we show an explanation?
//        if (ActivityCompat.shouldShowRequestPermissionRationale(
//                this,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        ) {
//
//            // Show an explanation to the user *asynchronously* -- don't block
//            // this thread waiting for the user's response! After the user
//            // sees the explanation, try again to request the permission.
//            AlertDialog.Builder(this)
//                .setTitle("Location Permission Needed")
//                .setMessage("This app needs the Location permission, please accept to use location functionality")
//                .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
//                    //Prompt the user once explanation has been shown
//                    ActivityCompat.requestPermissions(
//                        this@MapLocationActivity,
//                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                        MY_PERMISSIONS_REQUEST_LOCATION
//                    )
//                })
//                .create()
//                .show()
//
//
//        } else {
//            // No explanation needed, we can request the permission.
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                MY_PERMISSIONS_REQUEST_LOCATION
//            )
//        }
//    }
}