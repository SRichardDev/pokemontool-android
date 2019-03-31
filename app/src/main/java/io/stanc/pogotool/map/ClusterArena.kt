package io.stanc.pogotool.map

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import io.stanc.pogotool.firebase.data.FirebaseArena


class ClusterArena(id: String,
                   isEx: Boolean,
                    private val position: LatLng,
                    private val title: String = "",
                    private val snippet: String = "") : ClusterItem {

        val tag = ClusterArena.Tag(id, isEx)

        override fun getPosition(): LatLng {
            return position
        }

        override fun getTitle(): String {
            return title
        }

        override fun getSnippet(): String {
            return snippet
        }

        data class Tag(val id: String, val isEx: Boolean)

        companion object {

            fun new(arena: FirebaseArena): ClusterArena {

                val position = LatLng(arena.geoHash.toLocation().latitude, arena.geoHash.toLocation().longitude)
                val title = arena.name
                val snippet = ""

                return ClusterArena(arena.id, arena.isEX, position, title, snippet)
            }
        }
    }