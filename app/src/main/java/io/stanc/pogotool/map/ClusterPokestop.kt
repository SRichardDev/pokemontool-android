package io.stanc.pogotool.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash


class ClusterPokestop(val pokestop: FirebasePokestop,
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

        fun new(pokestop: FirebasePokestop): ClusterPokestop {

            val position = LatLng(pokestop.geoHash.toLocation().latitude, pokestop.geoHash.toLocation().longitude)
            val title = pokestop.name
            val snippet = ""

            return ClusterPokestop(pokestop, position, title, snippet)
        }
    }
}