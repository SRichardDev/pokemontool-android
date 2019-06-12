package io.stanc.pogotool.map

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.utils.IconFactory
import io.stanc.pogotool.utils.Kotlin
import io.stanc.pogotool.viewmodel.ArenaViewModel


class ClusterArenaRenderer(private val context: Context, map: GoogleMap,
                           clusterManager: ClusterManager<ClusterArena>
) : DefaultClusterRenderer<ClusterArena>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterArena>): Boolean {
        return cluster.items.filter { ArenaViewModel(it.arena).isArenaVisibleOnMap.get() == true }.size > 5
    }

    override fun onBeforeClusterItemRendered(item: ClusterArena?, markerOptions: MarkerOptions?) {
        Kotlin.safeLet(item, markerOptions) { clusterItem, markerOptions ->
            markerOptions.title(clusterItem.title).icon(getBitmapDescriptor(context, clusterItem.arena)).anchor(ANCHOR_X, ANCHOR_Y)
        }
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterItemRendered(clusterItem: ClusterArena?, marker: Marker?) {
        Kotlin.safeLet(clusterItem, marker) { _clusterItem, _marker ->
            _marker.tag = _clusterItem.arena
            _marker.isVisible = ArenaViewModel(_clusterItem.arena).isArenaVisibleOnMap.get() == true
        }
        super.onClusterItemRendered(clusterItem, marker)
    }

    companion object {

        private val TAG = javaClass.name

        private const val ICON_SIZE: Int = 100
        private const val INNER_ICON_SIZE: Int = 60
        private val ICON_CONFIG = IconFactory.IconSizeConfig(ICON_SIZE, INNER_ICON_SIZE)
        // TODO: calculate anchor, depending on header & footer text, the bottom of arena icon should be the map location position !
        private const val ANCHOR_X = 0.5f
        private const val ANCHOR_Y = 1.0f

        private fun getBitmapDescriptor(context: Context, arena: FirebaseArena): BitmapDescriptor {
            val arenaIconBitmap = arena.icon(context, ICON_CONFIG)
            return BitmapDescriptorFactory.fromBitmap(arenaIconBitmap)
        }

        private fun getBitmapDescriptor(context: Context, isEx: Boolean = false): BitmapDescriptor {
            val arenaIconBitmap = FirebaseArena.baseIcon(context, isEx, ICON_CONFIG)
            return BitmapDescriptorFactory.fromBitmap(arenaIconBitmap)
        }

        fun arenaMarkerOptions(context: Context, isEx: Boolean = false): MarkerOptions {
            return MarkerOptions().icon(getBitmapDescriptor(context, isEx)).anchor(ANCHOR_X, ANCHOR_Y)
        }

        class InfoWindowAdapter(context: Context): GoogleMap.InfoWindowAdapter {

            private val infoWindowView = LayoutInflater.from(context).inflate(R.layout.layout_info_window_arena, null, false)

            private val header = infoWindowView.findViewById(io.stanc.pogotool.R.id.info_window_arena_textview_header) as TextView
            private val subheader = infoWindowView.findViewById(io.stanc.pogotool.R.id.info_window_arena_textview_subheader) as TextView

            private val subheaderTextDefault = context.getText(R.string.map_info_window_arena_subheader_default)
            private val subheaderTextEx = context.getText(R.string.map_info_window_arena_subheader_ex)

            override fun getInfoContents(p0: Marker?): View? {
                return null
            }

            override fun getInfoWindow(p0: Marker?): View {
                p0?.let { marker ->
                    header.text = marker.title

                    (marker.tag as? FirebaseArena)?.let { arena ->
                        subheader.text = if (arena.isEX) subheaderTextEx else subheaderTextDefault
                    }
                }

                return infoWindowView
            }
        }
    }
}