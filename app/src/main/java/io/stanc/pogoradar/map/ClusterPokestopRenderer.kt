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
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.IconFactory
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.viewmodel.pokestop.PokestopViewModel


class ClusterPokestopRenderer(
    private val context: Context, map: GoogleMap,
    clusterManager: ClusterManager<ClusterPokestop>
) : DefaultClusterRenderer<ClusterPokestop>(context, map, clusterManager) {

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterPokestop>): Boolean {
        return cluster.items.filter { PokestopViewModel.new(it.pokestop, context).isPokestopVisibleOnMap.get() == true }.size > 8
    }

    override fun onBeforeClusterItemRendered(item: ClusterPokestop?, markerOptions: MarkerOptions?) {
        Kotlin.safeLet(item, markerOptions) { clusterItem, markerOptions ->
            val bitmapDescriptor = getBitmapDescriptor(context, clusterItem.pokestop, IconFactory.SizeMod.DEFAULT, clusterItem.showName)
            markerOptions.title(clusterItem.title).icon(bitmapDescriptor).anchor(ANCHOR_X, ANCHOR_Y)
        }
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterItemRendered(clusterItem: ClusterPokestop?, marker: Marker?) {
        Kotlin.safeLet(clusterItem, marker) { _clusterItem, _marker ->
            _marker.tag = _clusterItem.pokestop
            _marker.isVisible = PokestopViewModel.new(_clusterItem.pokestop, context).isPokestopVisibleOnMap.get() == true
        }
        super.onClusterItemRendered(clusterItem, marker)
    }

    companion object {

        private val TAG = javaClass.name

        private const val ANCHOR_X = 0.5f
        private const val ANCHOR_Y = 1.0f

        private fun getBitmapDescriptor(context: Context, pokestop: FirebasePokestop, sizeMod: IconFactory.SizeMod, showPokestopName: Boolean): BitmapDescriptor {
            val bm = MapIconFactory.pokestopIcon(context, pokestop, sizeMod, showFooterText = showPokestopName)
            return BitmapDescriptorFactory.fromBitmap(bm)
        }

        private fun getBitmapDescriptor(context: Context, sizeMod: IconFactory.SizeMod): BitmapDescriptor {
            val bm = MapIconFactory.pokestopIconBase(context, sizeMod)
            return BitmapDescriptorFactory.fromBitmap(bm)
        }

        fun pokestopMarkerOptions(context: Context, sizeMod: IconFactory.SizeMod = IconFactory.SizeMod.DEFAULT): MarkerOptions {
            return MarkerOptions().icon(getBitmapDescriptor(context, sizeMod)).anchor(ANCHOR_X, ANCHOR_Y)
        }

        class InfoWindowAdapter(context: Context): GoogleMap.InfoWindowAdapter {

            private val infoWindowView = LayoutInflater.from(context).inflate(R.layout.layout_info_window_pokestop, null, false)

            private val header = infoWindowView.findViewById(R.id.info_window_pokestop_textview_header) as TextView

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