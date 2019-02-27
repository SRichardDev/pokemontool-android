package io.stanc.pogotool.map

import com.google.maps.android.clustering.ClusterItem
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogotool.geohash.GeoHash


class ClusterPokestop(val id: String,
                      private val position: LatLng,
                      private val title: String = "",
                      private val snippet: String = "") : ClusterItem {

    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }

    companion object {

        fun new(id: String, geoHash: GeoHash, title: String = "", snippet: String = ""): ClusterPokestop {
            val position = LatLng(geoHash.toLocation().latitude, geoHash.toLocation().longitude)
            return ClusterPokestop(id, position, title, snippet)
        }
    }
}