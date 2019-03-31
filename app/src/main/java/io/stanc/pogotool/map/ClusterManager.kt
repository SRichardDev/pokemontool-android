package io.stanc.pogotool.map

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.utils.DelayedTrigger
import java.lang.ref.WeakReference


class ClusterManager(context: Context, googleMap: GoogleMap, private val delegate: MarkerDelegate) {

    private val TAG = javaClass.name

    private val pokestopClusterManager: ClusterManager<ClusterPokestop> = ClusterManager(context, googleMap)
    private val arenaClusterManager: ClusterManager<ClusterArena> = ClusterManager(context, googleMap)
    
    private val pokestopClustering = DelayedTrigger(200) { pokestopClusterManager.cluster() }
    private val arenaClustering = DelayedTrigger(200) { arenaClusterManager.cluster() }

    private val arenaInfoWindowAdapter = ClusterArenaRenderer.Companion.InfoWindowAdapter(context)
    private val pokestopInfoWindowAdapter = ClusterPokestopRenderer.Companion.InfoWindowAdapter(context)

    private val googleMapInfoWindowAdapter = object: GoogleMap.InfoWindowAdapter {
        override fun getInfoContents(p0: Marker?): View? {
            (p0?.tag as? ClusterArena.Tag)?.let {
                return arenaInfoWindowAdapter.getInfoContents(p0)
            }
            (p0?.tag as? ClusterPokestop.Tag)?.let {
                return pokestopInfoWindowAdapter.getInfoContents(p0)
            }

            return null
        }

        override fun getInfoWindow(p0: Marker?): View? {
            (p0?.tag as? ClusterArena.Tag)?.let {
                return arenaInfoWindowAdapter.getInfoWindow(p0)
            }
            (p0?.tag as? ClusterPokestop.Tag)?.let {
                return pokestopInfoWindowAdapter.getInfoWindow(p0)
            }

            return null
        }
    }

    init {
        pokestopClusterManager.renderer = ClusterPokestopRenderer(context, googleMap, pokestopClusterManager)
        arenaClusterManager.renderer = ClusterArenaRenderer(context, googleMap, arenaClusterManager)

        googleMap.setOnMarkerClickListener { this.onMarkerClicked(it) }
        googleMap.setOnInfoWindowClickListener {this.onInfoWindowClicked(it)}

        googleMap.setInfoWindowAdapter(googleMapInfoWindowAdapter)
    }

    /**
     * marker & info window
     */

    interface MarkerDelegate {
        fun onArenaInfoWindowClicked(id: String)
        fun onPokestopInfoWindowClicked(id: String)
    }

    private fun onInfoWindowClicked(marker: Marker) {
        val tag = marker.tag

        (tag as? ClusterArena.Tag)?.let {
            delegate.onArenaInfoWindowClicked(it.id)
        }

        (tag as? ClusterPokestop.Tag)?.let {
            delegate.onPokestopInfoWindowClicked(it.id)
        }
    }

    private fun onMarkerClicked(marker: Marker): Boolean {
        var handled = false

        pokestopClusterManager.let {
            if (it.onMarkerClick(marker)) {
                handled = true
            }
        }

        if (!handled) {
            arenaClusterManager.let {
                if (it.onMarkerClick(marker)) {
                    handled = true
                }
            }
        }

        return handled
    }

    /**
     *
     */

    fun onCameraIdle() {
        pokestopClusterManager.onCameraIdle()
        arenaClusterManager.onCameraIdle()
    }

    /**
     * firebase items
     */

    val pokestopDelegate = object : FirebaseDatabase.Delegate<FirebasePokestop> {
        override fun onItemAdded(item: FirebasePokestop) {

            if (!items.containsKey(item.id)) {
                val clusterItem = ClusterPokestop.new(item)
                pokestopClusterManager.addItem(clusterItem)
                pokestopClustering.trigger()
                items[clusterItem.tag.id] = WeakReference(clusterItem)
            }
        }

        override fun onItemChanged(item: FirebasePokestop) {

            onItemRemoved(item)
            onItemAdded(item)
        }

        override fun onItemRemoved(item: FirebasePokestop) {

            items[item.id]?.get()?.let {
                pokestopClusterManager.removeItem(it)
                pokestopClustering.trigger()
            }
            items.remove(item.id)
        }

        private val items: HashMap<String, WeakReference<ClusterPokestop>> = HashMap()
    }

    val arenaDelegate = object : FirebaseDatabase.Delegate<FirebaseArena> {
        override fun onItemAdded(item: FirebaseArena) {

            if (!items.containsKey(item.id)) {
                val clusterItem = ClusterArena.new(item)
                arenaClusterManager.addItem(clusterItem)
                arenaClustering.trigger()
                items[clusterItem.tag.id] = WeakReference(clusterItem)
            }
        }

        override fun onItemChanged(item: FirebaseArena) {

            onItemRemoved(item)
            onItemAdded(item)
        }

        override fun onItemRemoved(item: FirebaseArena) {

            items[item.id]?.get()?.let {
                arenaClusterManager.removeItem(it)
                arenaClustering.trigger()
            }
            items.remove(item.id)
        }

        private val items: HashMap<String, WeakReference<ClusterArena>> = HashMap()
    }
}