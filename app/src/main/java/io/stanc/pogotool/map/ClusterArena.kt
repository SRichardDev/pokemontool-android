package io.stanc.pogotool.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import io.stanc.pogotool.firebase.data.FirebaseArena


class ClusterArena(val id: String,
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

                val id = arena.id
                val position = LatLng(arena.geoHash.toLocation().latitude, arena.geoHash.toLocation().longitude)
                val title = arena.name
                val snippet = ""

                return ClusterArena(id, position, title, snippet)
            }
        }
    }