package io.stanc.pogoradar.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import io.stanc.pogoradar.firebase.node.FirebaseArena


class ClusterArena(val arena: FirebaseArena,
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

            fun new(arena: FirebaseArena): ClusterArena {

                val position = LatLng(arena.geoHash.toLocation().latitude, arena.geoHash.toLocation().longitude)
                val title = arena.name
                val snippet = ""

                return ClusterArena(arena, position, title, snippet)
            }
        }
    }