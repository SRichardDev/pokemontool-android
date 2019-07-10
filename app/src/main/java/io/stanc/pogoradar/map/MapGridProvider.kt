package io.stanc.pogoradar.map

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.GEO_HASH_AREA_PRECISION
import io.stanc.pogoradar.geohash.GeoHash
import java.lang.ref.WeakReference

class MapGridProvider(private val googleMap: GoogleMap, context: Context) {

    private val TAG = javaClass.name
    private val geoHashList: HashMap<GeoHash, Polygon> = HashMap()
    private val context = WeakReference(context)


    fun geoHashes(): List<GeoHash> {
        return geoHashList.keys.toList()
    }

    fun geoHashExists(geoHash: GeoHash): Boolean {
        return geoHashList.containsKey(geoHash)
    }

    fun toggleGeoHashGrid(latlng: LatLng) {
        val geoHash = GeoHash(
            latlng.latitude,
            latlng.longitude,
            GEO_HASH_AREA_PRECISION
        )
        toggleGeoHashGrid(geoHash)
    }

    fun toggleGeoHashGrid(geoHash: GeoHash) {

        if (geoHashExists(geoHash)) {
            removeGeoHashGrid(geoHash)
        } else {
            showGeoHashGrid(geoHash)
        }
    }


    fun showGeoHashGridList() {
        geoHashList.keys.forEach { showGeoHashGrid(it) }
    }

    fun showGeoHashGrid(location: Location) {
        val geoHash = GeoHash(location, GEO_HASH_AREA_PRECISION)
        showGeoHashGrid(geoHash)
    }

    fun showGeoHashGrid(latlng: LatLng) {
        val geoHash = GeoHash(
            latlng.latitude,
            latlng.longitude,
            GEO_HASH_AREA_PRECISION
        )
        showGeoHashGrid(geoHash)
    }

    fun showGeoHashGrid(geoHash: GeoHash) {

        if (geoHashExists(geoHash)) {
            geoHashList[geoHash]?.remove()
        }

        val polygonRect = PolygonOptions()
            .strokeWidth(5.0f)
            .add(
                LatLng(geoHash.boundingBox.bottomLeft.latitude, geoHash.boundingBox.bottomLeft.longitude),
                LatLng(geoHash.boundingBox.topLeft.latitude, geoHash.boundingBox.topLeft.longitude),
                LatLng(geoHash.boundingBox.topRight.latitude, geoHash.boundingBox.topRight.longitude),
                LatLng(geoHash.boundingBox.bottomRight.latitude, geoHash.boundingBox.bottomRight.longitude),
                LatLng(geoHash.boundingBox.bottomLeft.latitude, geoHash.boundingBox.bottomLeft.longitude)
            )

        context.get()?.let {
            polygonRect.fillColor(ContextCompat.getColor(it, R.color.colorGeoHashArea))
            polygonRect.strokeColor(ContextCompat.getColor(it, R.color.redCancel))
        }

        googleMap.addPolygon(polygonRect)?.let { geoHashList[geoHash] = it }
    }


    fun removeGeoHashGrid(geoHash: GeoHash) {
        geoHashList.remove(geoHash)?.remove()
    }

    fun clearGeoHashGridList() {
        for (polygon in geoHashList.values) {
            polygon.remove()
        }
        geoHashList.clear()
    }
}