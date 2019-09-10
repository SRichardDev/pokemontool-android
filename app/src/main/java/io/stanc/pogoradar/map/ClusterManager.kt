package io.stanc.pogoradar.map

//import com.arsy.maps_library.MapRipple
import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogoradar.MapFilterSettings
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.DelayedTrigger
import io.stanc.pogoradar.viewmodel.arena.isArenaVisibleOnMap
import io.stanc.pogoradar.viewmodel.pokestop.PokestopViewModel
import java.lang.ref.WeakReference


class ClusterManager(context: Context, googleMap: GoogleMap, private val delegate: MarkerDelegate) {

    private val TAG = javaClass.name

    private val pokestopClusterManager: ClusterManager<ClusterPokestop> = ClusterManager(context, googleMap)
    private val arenaClusterManager: ClusterManager<ClusterArena> = ClusterManager(context, googleMap)

    private val clusterPokestops: HashMap<String, WeakReference<ClusterPokestop>> = HashMap()
    private val clusterArenas: HashMap<String, WeakReference<ClusterArena>> = HashMap()
    
    private val pokestopClustering = DelayedTrigger(0) { pokestopClusterManager.cluster() }
    private val arenaClustering = DelayedTrigger(0) { arenaClusterManager.cluster() }
    // https://github.com/aarsy/GoogleMapsAnimations
//    private val arenaRippleAnimations = HashMap<Any, WeakReference<MapRipple>>()
//    private val context = WeakReference(context)
//    private val map = WeakReference(googleMap)

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

    private val mapSettingsObserver = object : MapFilterSettings.MapSettingObserver {

        override fun onArenasVisibilityDidChange() {
            val markers = arenaClusterManager.markerCollection.markers
            for (marker in markers) {
                (marker.tag as? FirebaseArena)?.let {
                    marker.isVisible = isArenaVisibleOnMap(it)
                }
            }
            arenaClusterManager.cluster()
        }

        override fun onPokestopsVisibilityDidChange() {
            val markers = pokestopClusterManager.markerCollection.markers
            for (marker in markers) {
                (marker.tag as? FirebasePokestop)?.let {
                    marker.isVisible = PokestopViewModel.new(it, context).isPokestopVisibleOnMap.get() == true
                }
            }
            pokestopClusterManager.cluster()
        }
    }

    init {
        pokestopClusterManager.renderer = ClusterPokestopRenderer(context, googleMap, pokestopClusterManager)
        arenaClusterManager.renderer = ClusterArenaRenderer(context, googleMap, arenaClusterManager)
        // cluster animation only
        pokestopClusterManager.setAnimation(true)
        arenaClusterManager.setAnimation(true)

        googleMap.setOnInfoWindowClickListener { this.onInfoWindowClicked(it) }
        googleMap.setInfoWindowAdapter(googleMapInfoWindowAdapter)

        MapFilterSettings.addObserver(mapSettingsObserver)
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

    /**
     * map callbacks
     */

    fun onCameraIdle() {
        pokestopClusterManager.onCameraIdle()
        arenaClusterManager.onCameraIdle()
    }

    /**
     * Pokestops
     */

    fun showNewAndRemoveOldPokestops(pokestops: List<FirebasePokestop>) {

        val oldPokestopIds = clusterPokestops.keys.minus(pokestops.map { it.id })

        pokestops.forEach { pokestop ->

            // add new pokestops
            if (!clusterPokestops.containsKey(pokestop.id)) {
                val clusterItem = ClusterPokestop.new(pokestop)
                pokestopClusterManager.addItem(clusterItem)
                clusterPokestops[clusterItem.pokestop.id] = WeakReference(clusterItem)
            }
        }

        // remove old ones
        oldPokestopIds.forEach { pokestopId ->
            clusterPokestops.remove(pokestopId)?.get()?.let { clusterPokestop ->
                pokestopClusterManager.removeItem(clusterPokestop)
            }
        }

        pokestopClustering.trigger()
    }

    fun showPokestops(pokestops: List<FirebasePokestop>) {
        pokestops.forEach { pokestop ->

            if (!clusterPokestops.containsKey(pokestop.id)) {
                val clusterItem = ClusterPokestop.new(pokestop)
                pokestopClusterManager.addItem(clusterItem)
                clusterPokestops[clusterItem.pokestop.id] = WeakReference(clusterItem)
            }
        }
        pokestopClustering.trigger()
    }

    fun removeAllPokestops() {
        pokestopClusterManager.clearItems()
        clusterPokestops.clear()
        pokestopClustering.trigger()
    }

    /**
     * Arenas
     */

    fun showNewAndRemoveOldArenas(arenas: List<FirebaseArena>) {

        val oldArenaIds = clusterArenas.keys.minus(arenas.map { it.id })

        arenas.forEach { arena ->

            // add new arenas
            if (!clusterArenas.containsKey(arena.id)) {
                val clusterItem = ClusterArena.new(arena)
                arenaClusterManager.addItem(clusterItem)
                clusterArenas[clusterItem.arena.id] = WeakReference(clusterItem)
            }
        }

        // remove old ones
        oldArenaIds.forEach { arenaId ->
            clusterArenas.remove(arenaId)?.get()?.let { clusterArena ->
                arenaClusterManager.removeItem(clusterArena)
            }
        }

        arenaClustering.trigger()
    }

    fun showArenas(arenas: List<FirebaseArena>) {
        arenas.forEach { arena ->

            if (!clusterArenas.containsKey(arena.id)) {
                val clusterItem = ClusterArena.new(arena)
                arenaClusterManager.addItem(clusterItem)
                clusterArenas[clusterItem.arena.id] = WeakReference(clusterItem)
            }
        }
        arenaClustering.trigger()
    }

    fun removeAllArenas() {
        arenaClusterManager.clearItems()
        clusterArenas.clear()
        arenaClustering.trigger()
    }

    /**
     * animation
     */

    private fun startAnimation(arena: FirebaseArena) {
        // TODO: search for performant googlemap pulse animation
//        Kotlin.safeLet(map.get(), context.get()) { map, context ->
//
//            stopAnimation(arena)
//
//            val mapRipple = mapRipple(map, arena, context)
//            arenaRippleAnimations[arena.id] = WeakReference(mapRipple)
//            mapRipple.startRippleMapAnimation()
//
//        } ?: run {
//            Log.e(TAG, "could not start map animation, because map.get(): ${map.get()} or context.get(): ${context.get()}")
//        }
    }

    private fun stopAnimation(arena: FirebaseArena) {
//        arenaRippleAnimations.remove(arena.id)?.get()?.stopRippleMapAnimation()
    }

//    private fun mapRipple(googleMap: GoogleMap, arena: FirebaseArena, context: Context): MapRipple {
//
//        return MapRipple(googleMap, arena.geoHash.toLatLng(), context).apply {
//
//            withNumberOfRipples(5)
//            withFillColor(Color.RED)
//            withStrokeColor(Color.TRANSPARENT)
//            withStrokewidth(0)
//            withDistance(200.0)
//            withRippleDuration(5000)
//            withDurationBetweenTwoRipples(1000)
//            withTransparency(0.8f)
//        }
//    }
}