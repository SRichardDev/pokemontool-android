package io.stanc.pogotool.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.annotation.DrawableRes
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
import io.stanc.pogotool.utils.KotlinUtils


class ClusterArenaRenderer(private val context: Context, map: GoogleMap,
                           clusterManager: ClusterManager<ClusterArena>
) : DefaultClusterRenderer<ClusterArena>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterArena>): Boolean {
        return cluster.size > 5 // when count of markers is more than 5, render as cluster
    }

    override fun onBeforeClusterItemRendered(item: ClusterArena?, markerOptions: MarkerOptions?) {
        markerOptions?.title(item?.title)?.icon(icon(context))?.anchor(ANCHOR_X, ANCHOR_Y)
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterItemRendered(clusterItem: ClusterArena?, marker: Marker?) {
        KotlinUtils.safeLet(clusterItem, marker) { _clusterItem, _marker ->
            _marker.tag = _clusterItem.tag
        }
        super.onClusterItemRendered(clusterItem, marker)
    }

    companion object {

        private val TAG = javaClass.name

        private const val ICON_HEIGHT: Int = 75
        private const val ICON_WIDTH: Int = 75
        private const val ANCHOR_X = 0.5f
        private const val ANCHOR_Y = 1.0f

        private fun icon(context: Context) = getBitmapDescriptor(context, R.drawable.icon_arenaex_30dp)

        private fun getBitmapDescriptor(context: Context, @DrawableRes id: Int): BitmapDescriptor {
            val vectorDrawable = context.getDrawable(id)

            vectorDrawable?.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT)
            val bm = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            vectorDrawable?.draw(canvas)

            return BitmapDescriptorFactory.fromBitmap(bm)
        }

        fun arenaMarkerOptions(context: Context): MarkerOptions {
            return MarkerOptions().icon(icon(context)).anchor(ANCHOR_X, ANCHOR_Y)
        }

        class InfoWindowAdapter(context: Context): GoogleMap.InfoWindowAdapter {

            private val infoWindowView = LayoutInflater.from(context).inflate(R.layout.layout_info_window_arena, null)

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

                    (marker.tag as? ClusterArena.Tag)?.let { arenaTag ->
                        subheader.text = if (arenaTag.isEx) subheaderTextEx else subheaderTextDefault
                    }
                }

                return infoWindowView
            }
        }
    }
}