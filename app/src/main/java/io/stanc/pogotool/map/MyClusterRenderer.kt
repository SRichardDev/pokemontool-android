package io.stanc.pogotool.map

import com.google.android.gms.maps.model.Marker
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.annotation.DrawableRes
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import io.stanc.pogotool.R


class MyClusterRenderer(
    private val context: Context, map: GoogleMap,
    clusterManager: ClusterManager<ClusterPokestop>
) : DefaultClusterRenderer<ClusterPokestop>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: ClusterPokestop?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, markerOptions)

        markerOptions?.title("")?.icon(getBitmapDescriptor(R.drawable.icon_pstop_30dp))// for marker
    }

    override fun onClusterItemRendered(clusterItem: ClusterPokestop?, marker: Marker?) {
        super.onClusterItemRendered(clusterItem, marker)

    }

    private fun getBitmapDescriptor(@DrawableRes id: Int): BitmapDescriptor {
        val vectorDrawable = context.getDrawable(id)
//        val h = ((int) Utils?.convertDpToPixel(42, context));
//        val w = ((int) Utils?.convertDpToPixel(25, context));
        vectorDrawable?.setBounds(0, 0, 50, 50)
        val bm = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable?.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
    }
}