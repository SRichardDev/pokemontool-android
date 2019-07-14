package io.stanc.pogoradar.map

import android.content.Context
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
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.utils.IconFactory
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.viewmodel.ArenaViewModel


class ClusterArenaRenderer(private val context: Context, map: GoogleMap,
                           clusterManager: ClusterManager<ClusterArena>
) : DefaultClusterRenderer<ClusterArena>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterArena>): Boolean {
        return cluster.items.filter { ArenaViewModel.new(it.arena).isArenaVisibleOnMap.get() == true }.size > 3
    }

    override fun onBeforeClusterItemRendered(item: ClusterArena?, markerOptions: MarkerOptions?) {
        Kotlin.safeLet(item, markerOptions) { clusterItem, markerOptions ->
            markerOptions.title(clusterItem.title).icon(getBitmapDescriptor(context, clusterItem.arena, IconFactory.SizeMod.DEFAULT)).anchor(ANCHOR_X, ANCHOR_Y)
        }
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterItemRendered(clusterItem: ClusterArena?, marker: Marker?) {
        Kotlin.safeLet(clusterItem, marker) { _clusterItem, _marker ->
            _marker.tag = _clusterItem.arena
            _marker.isVisible = ArenaViewModel.new(_clusterItem.arena).isArenaVisibleOnMap.get() == true
        }
        super.onClusterItemRendered(clusterItem, marker)
    }

    companion object {

        private val TAG = javaClass.name

        // TODO: calculate anchor, depending on header & footer text, the bottom of arena arenaIcon should be the map location position !
        private const val ANCHOR_X = 0.5f
        private const val ANCHOR_Y = 1.0f

        private fun getBitmapDescriptor(context: Context, arena: FirebaseArena, sizeMod: IconFactory.SizeMod): BitmapDescriptor {
            val arenaIconBitmap = MapIconFactory.arenaIcon(context, arena, sizeMod)
            return BitmapDescriptorFactory.fromBitmap(arenaIconBitmap)
        }

        private fun getBitmapDescriptor(context: Context, isEx: Boolean = false, sizeMod: IconFactory.SizeMod): BitmapDescriptor {
            val arenaIconBitmap = MapIconFactory.arenaIconBase(context, isEx, sizeMod)
            return BitmapDescriptorFactory.fromBitmap(arenaIconBitmap)
        }

        fun arenaMarkerOptions(context: Context, isEx: Boolean = false, sizeMod: IconFactory.SizeMod = IconFactory.SizeMod.DEFAULT): MarkerOptions {
            return MarkerOptions().icon(getBitmapDescriptor(context, isEx, sizeMod)).anchor(ANCHOR_X, ANCHOR_Y)
        }

        class InfoWindowAdapter(context: Context): GoogleMap.InfoWindowAdapter {

            private val infoWindowView = LayoutInflater.from(context).inflate(R.layout.layout_info_window_arena, null, false)

            private val header = infoWindowView.findViewById(R.id.info_window_arena_textview_header) as TextView
            private val subheader = infoWindowView.findViewById(R.id.info_window_arena_textview_subheader) as TextView

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