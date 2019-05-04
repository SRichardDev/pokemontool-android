package io.stanc.pogotool.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
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
import io.stanc.pogotool.utils.KotlinUtils


class ClusterArenaRenderer(private val context: Context, map: GoogleMap,
                           clusterManager: ClusterManager<ClusterArena>
) : DefaultClusterRenderer<ClusterArena>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterArena>): Boolean {
        return cluster.size > 5 // when count of markers is more than 5, render as cluster
    }

    override fun onBeforeClusterItemRendered(item: ClusterArena?, markerOptions: MarkerOptions?) {
        markerOptions?.title(item?.title)?.icon(arenaIcon(context, item?.arena))?.anchor(ANCHOR_X, ANCHOR_Y)
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterItemRendered(clusterItem: ClusterArena?, marker: Marker?) {
        KotlinUtils.safeLet(clusterItem, marker) { _clusterItem, _marker ->
            _marker.tag = _clusterItem.arena
        }
        super.onClusterItemRendered(clusterItem, marker)
    }

    companion object {

        private val TAG = javaClass.name

        private const val ICON_HEIGHT: Int = 100
        private const val ICON_WIDTH: Int = 100
        private const val INNER_ICON_HEIGHT: Int = 60
        private const val INNER_ICON_WIDTH: Int = 60
        private const val ANCHOR_X = 0.5f
        private const val ANCHOR_Y = 1.0f

        private fun arenaIcon(context: Context, arena: FirebaseArena?): BitmapDescriptor {

            val foregroundDrawable = RaidBossImageMapper.raidDrawable(context, arena)
            Log.i(TAG, "Debug:: arenaIcon(arena: $arena), foregroundDrawable: $foregroundDrawable")

            return arena?.isEX?.let { isExArena ->

                if (isExArena) {
                    getBitmapDescriptor(context, R.drawable.icon_arena_ex_30dp, foregroundDrawable)
                } else {
                    getBitmapDescriptor(context, R.drawable.icon_arena_30dp, foregroundDrawable)
                }

            } ?: kotlin.run {
                getBitmapDescriptor(context, R.drawable.icon_arena_30dp, foregroundDrawable)
            }

        }

        private fun arenaBaseIcon(context: Context, isExArena: Boolean): BitmapDescriptor {

            return if (isExArena) {
                getBitmapDescriptor(context, R.drawable.icon_arena_ex_30dp)
            } else {
                getBitmapDescriptor(context, R.drawable.icon_arena_30dp)
            }
        }

        private fun getBitmapDescriptor(context: Context, @DrawableRes backgroundDrawableRes: Int, foregroundDrawable: Drawable? = null): BitmapDescriptor {

            val bitmap = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val backgroundDrawable = context.getDrawable(backgroundDrawableRes)
            backgroundDrawable?.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT)
            backgroundDrawable?.draw(canvas)

            foregroundDrawable?.let {

                val marginLeft = (ICON_WIDTH-INNER_ICON_WIDTH)/2
                val marginTop = (ICON_HEIGHT-INNER_ICON_HEIGHT)/2
                it.setBounds(marginLeft, marginTop, ICON_WIDTH-marginLeft, ICON_HEIGHT-marginTop)
                it.draw(canvas)
            }

//            Log.i(TAG, "Debug:: getBitmapDescriptor(), backgroundDrawable: $backgroundDrawable, foregroundDrawable: $foregroundDrawable")
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        fun arenaMarkerOptions(context: Context, isEx: Boolean = false): MarkerOptions {
            return MarkerOptions().icon(arenaBaseIcon(context, isEx)).anchor(ANCHOR_X, ANCHOR_Y)
        }

        class InfoWindowAdapter(context: Context): GoogleMap.InfoWindowAdapter {

            // TODO: windows have different size, depending on header title
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

                    (marker.tag as? FirebaseArena)?.let { arena ->
                        subheader.text = if (arena.isEX) subheaderTextEx else subheaderTextDefault
                    }
                }

                return infoWindowView
            }
        }
    }
}