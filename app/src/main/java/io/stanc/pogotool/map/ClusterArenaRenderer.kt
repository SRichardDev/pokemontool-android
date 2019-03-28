package io.stanc.pogotool.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.annotation.DrawableRes
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import io.stanc.pogotool.R

class ClusterArenaRenderer(private val context: Context, map: GoogleMap,
                           clusterManager: ClusterManager<ClusterArena>
) : DefaultClusterRenderer<ClusterArena>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterArena>): Boolean {
        return cluster.size > 5 // when count of markers is more than 5, render as cluster
    }

    override fun onBeforeClusterItemRendered(item: ClusterArena?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        markerOptions?.title(item?.title)?.icon(icon(context))?.anchor(ANCHOR_X, ANCHOR_Y)
    }

    companion object {

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
    }
}