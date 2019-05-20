package io.stanc.pogotool.map

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
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
            (p0?.tag as? FirebaseArena)?.let {
                return arenaInfoWindowAdapter.getInfoContents(p0)
            }
            (p0?.tag as? FirebasePokestop)?.let {
                return pokestopInfoWindowAdapter.getInfoContents(p0)
            }

            return null
        }

        override fun getInfoWindow(p0: Marker?): View? {
            (p0?.tag as? FirebaseArena)?.let {
                return arenaInfoWindowAdapter.getInfoWindow(p0)
            }
            (p0?.tag as? FirebasePokestop)?.let {
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
        fun onArenaInfoWindowClicked(arena: FirebaseArena)
        fun onPokestopInfoWindowClicked(pokestop: FirebasePokestop)
    }

    private fun onInfoWindowClicked(marker: Marker) {
        val tag = marker.tag

        (tag as? FirebaseArena)?.let {
            delegate.onArenaInfoWindowClicked(it)
        }

        (tag as? FirebasePokestop)?.let {
            delegate.onPokestopInfoWindowClicked(it)
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
                items[clusterItem.pokestop.id] = WeakReference(clusterItem)
            }
        }

        override fun onItemChanged(item: FirebasePokestop) {
            Log.v(TAG, "Debug:: onItemChanged($item)")
            onItemRemoved(item.id)
            onItemAdded(item)
            pokestopClustering.trigger()
        }

        override fun onItemRemoved(itemId: String) {

            items[itemId]?.get()?.let {
                pokestopClusterManager.removeItem(it)
            }
            items.remove(itemId)
        }

        private val items: HashMap<String, WeakReference<ClusterPokestop>> = HashMap()
    }

    val arenaDelegate = object : FirebaseDatabase.Delegate<FirebaseArena> {

        override fun onItemAdded(item: FirebaseArena) {

            if (!items.containsKey(item.id)) {
                val clusterItem = ClusterArena.new(item)
                arenaClusterManager.addItem(clusterItem)
                items[clusterItem.arena.id] = WeakReference(clusterItem)
            }
        }

        override fun onItemChanged(item: FirebaseArena) {
            Log.v(TAG, "Debug:: onItemChanged($item)")
            onItemRemoved(item.id)
            onItemAdded(item)
            arenaClustering.trigger()
        }

        override fun onItemRemoved(itemId: String) {

            items[itemId]?.get()?.let {
                arenaClusterManager.removeItem(it)
            }
            items.remove(itemId)
        }

        private val items: HashMap<String, WeakReference<ClusterArena>> = HashMap()
    }
}