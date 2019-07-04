package io.stanc.pogoradar.map

import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.GEO_HASH_AREA_PRECISION
import io.stanc.pogoradar.geohash.GeoHash

class MapGridProvider(private val googleMap: GoogleMap) {

    private val TAG = javaClass.name
    private val geoHashList: HashMap<GeoHash, Polygon> = HashMap()


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

        if (geoHashList.contains(geoHash)) {
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