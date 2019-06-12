package io.stanc.pogotool.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import io.stanc.pogotool.utils.Kotlin
import io.stanc.pogotool.viewmodel.PokestopViewModel


class ClusterPokestopRenderer(
    private val context: Context, map: GoogleMap,
    clusterManager: ClusterManager<ClusterPokestop>
) : DefaultClusterRenderer<ClusterPokestop>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterPokestop>): Boolean {
        return cluster.items.filter { PokestopViewModel(it.pokestop).isPokestopVisibleOnMap.get() == true }.size > 5
    }

    override fun onBeforeClusterItemRendered(item: ClusterPokestop?, markerOptions: MarkerOptions?) {
        markerOptions?.title(item?.title)?.icon(icon(context))?.anchor(ANCHOR_X, ANCHOR_Y)
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterItemRendered(clusterItem: ClusterPokestop?, marker: Marker?) {
        Kotlin.safeLet(clusterItem, marker) { _clusterItem, _marker ->
            _marker.tag = _clusterItem.pokestop
            _marker.isVisible = PokestopViewModel(_clusterItem.pokestop).isPokestopVisibleOnMap.get() == true
        }
        super.onClusterItemRendered(clusterItem, marker)
    }

    companion object {

        private val TAG = javaClass.name

        private const val ICON_HEIGHT: Int = 75
        private const val ICON_WIDTH: Int = 25
        private const val ANCHOR_X = 0.5f
        private const val ANCHOR_Y = 1.0f

        private fun icon(context: Context) = getBitmapDescriptor(context, R.drawable.icon_pstop_30dp)

        private fun getBitmapDescriptor(context: Context, @DrawableRes id: Int): BitmapDescriptor {
            val vectorDrawable = context.getDrawable(id)

            vectorDrawable?.setBounds(0, 0, ICON_WIDTH, ICON_HEIGHT)
            val bm = Bitmap.createBitmap(ICON_WIDTH, ICON_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            vectorDrawable?.draw(canvas)

            return BitmapDescriptorFactory.fromBitmap(bm)
        }

        fun pokestopMarkerOptions(context: Context): MarkerOptions {
            return MarkerOptions().icon(icon(context)).anchor(ANCHOR_X, ANCHOR_Y)
        }

        class InfoWindowAdapter(context: Context): GoogleMap.InfoWindowAdapter {

            private val infoWindowView = LayoutInflater.from(context).inflate(R.layout.layout_info_window_pokestop, null, false)

            private val header = infoWindowView.findViewById(io.stanc.pogotool.R.id.info_window_pokestop_textview_header) as TextView

            override fun getInfoContents(p0: Marker?): View? {
                return null
            }

            override fun getInfoWindow(p0: Marker?): View {
                p0?.let { header.text = it.title }
                return infoWindowView
            }
        }
    }
}