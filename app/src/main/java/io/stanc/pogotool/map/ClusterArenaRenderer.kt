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

    private val iconSize: Int = 75

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterArena>): Boolean {
        return cluster.size > 5 // when count of markers is more than 5, render as cluster
    }

    override fun onBeforeClusterItemRendered(item: ClusterArena?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        markerOptions?.title(item?.title)?.icon(getBitmapDescriptor(R.drawable.icon_arenaex_30dp))?.anchor(0.5f, 1.0f)
    }

    private fun getBitmapDescriptor(@DrawableRes id: Int): BitmapDescriptor {
        val vectorDrawable = context.getDrawable(id)

        vectorDrawable?.setBounds(0, 0, iconSize, iconSize)
        val bm = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable?.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bm)
    }
}