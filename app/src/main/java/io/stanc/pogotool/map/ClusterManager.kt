package io.stanc.pogotool.map

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.utils.DelayedTrigger
import java.lang.ref.WeakReference

class ClusterManager {

    private val TAG = javaClass.name

    /**
     * setup
     */

    fun setup(context: Context, map: GoogleMap) {

        pokestopClusterManager = ClusterManager(context, map)
        pokestopClusterManager?.let { it.renderer = ClusterPokestopRenderer(context, map, it) }

        arenaClusterManager = ClusterManager(context, map)
        arenaClusterManager?.let { it.renderer = ClusterArenaRenderer(context, map, it) }
    }

    private var pokestopClusterManager: ClusterManager<ClusterPokestop>? = null
    private val pokestopClustering = DelayedTrigger(200) { pokestopClusterManager?.cluster() }

    private var arenaClusterManager: ClusterManager<ClusterArena>? = null
    private val arenaClustering = DelayedTrigger(200) { arenaClusterManager?.cluster() }

    /**
     *
     */

    fun onMarkerClick(marker: Marker): Boolean {

        var handled = false

        pokestopClusterManager?.let {
            handled = it.onMarkerClick(marker)
        }

        if (!handled) {
            arenaClusterManager?.let {
                handled = it.onMarkerClick(marker)
            }
        }

        return handled
    }

    fun onCameraIdle() {
        pokestopClusterManager?.onCameraIdle()
        arenaClusterManager?.onCameraIdle()
    }

    /**
     * delegates
     */

    val pokestopDelegate = object : FirebaseDatabase.Delegate<FirebasePokestop> {
        override fun onItemAdded(item: FirebasePokestop) {

            if (!items.containsKey(item.id)) {
                val clusterItem = ClusterPokestop.new(item)
                pokestopClusterManager?.addItem(clusterItem)
                pokestopClustering.trigger()
                items[clusterItem.id] = WeakReference(clusterItem)
            }
        }

        override fun onItemChanged(item: FirebasePokestop) {

            onItemRemoved(item)
            onItemAdded(item)
        }

        override fun onItemRemoved(item: FirebasePokestop) {

            items[item.id]?.get()?.let {
                pokestopClusterManager?.removeItem(it)
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
                arenaClusterManager?.addItem(clusterItem)
                arenaClustering.trigger()
                items[clusterItem.id] = WeakReference(clusterItem)
            }
        }

        override fun onItemChanged(item: FirebaseArena) {

            onItemRemoved(item)
            onItemAdded(item)
        }

        override fun onItemRemoved(item: FirebaseArena) {

            items[item.id]?.get()?.let {
                arenaClusterManager?.removeItem(it)
                arenaClustering.trigger()
            }
            items.remove(item.id)
        }

        private val items: HashMap<String, WeakReference<ClusterArena>> = HashMap()
    }
}