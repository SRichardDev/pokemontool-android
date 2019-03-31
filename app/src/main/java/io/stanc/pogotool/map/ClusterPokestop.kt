package io.stanc.pogotool.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import io.stanc.pogotool.firebase.data.FirebasePokestop


class ClusterPokestop(id: String,
                      private val position: LatLng,
                      private val title: String = "",
                      private val snippet: String = "") : ClusterItem {

    val tag = ClusterPokestop.Tag(id)

    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }

    data class Tag(val id: String)

    companion object {

        fun new(pokestop: FirebasePokestop): ClusterPokestop {

            val position = LatLng(pokestop.geoHash.toLocation().latitude, pokestop.geoHash.toLocation().longitude)
            val title = pokestop.name
            val snippet = ""

            return ClusterPokestop(pokestop.id, position, title, snippet)
        }
    }
}