package io.stanc.pogoradar.map

import android.content.Context
import android.view.View
//import com.arsy.maps_library.MapRipple
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogoradar.MapFilterSettings
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.DelayedTrigger
import java.lang.ref.WeakReference
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.RefreshTimer
import io.stanc.pogoradar.utils.TimeCalculator
import io.stanc.pogoradar.viewmodel.arena.*
import io.stanc.pogoradar.viewmodel.pokestop.PokestopViewModel


class ClusterManager(context: Context, googleMap: GoogleMap, private val delegate: MarkerDelegate) {

    private val TAG = javaClass.name

    private val pokestopClusterManager: ClusterManager<ClusterPokestop> = ClusterManager(context, googleMap)
    private val arenaClusterManager: ClusterManager<ClusterArena> = ClusterManager(context, googleMap)
    
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

                startArenaRefreshTimerIfRaidAnnounced(item)
            }
        }

        override fun onItemChanged(item: FirebaseArena) {
            onItemRemoved(item.id)
            onItemAdded(item)
            arenaClustering.trigger()
        }

        override fun onItemRemoved(itemId: String) {

            items[itemId]?.get()?.let {
                arenaClusterManager.removeItem(it)
                stopRefreshTimer(it.arena)
            }
            items.remove(itemId)
        }

        private val items: HashMap<String, WeakReference<ClusterArena>> = HashMap()
    }

    /**
     * timer & animation
     */

    private fun startArenaRefreshTimerIfRaidAnnounced(arena: FirebaseArena) {

        arena.raid?.let { raid ->

            if (currentRaidState(raid) != RaidState.NONE) {

                Kotlin.safeLet(raidTime(raid), (raid.timestamp as? Long)) { raidTime, timestamp ->
                    startRefreshTimer(arena, raidTime, timestamp)
                }
            }
        }
    }

    private fun startRefreshTimer(arena: FirebaseArena, raidTime: String, timestamp: Long) {

        TimeCalculator.minutesUntil(timestamp, raidTime)?.let { minutes ->

            if (minutes > 0) {
                startAnimation(arena)
                RefreshTimer.run(minutes, arena.id, onFinished = {
                    stopAnimation(arena)
                    arenaDelegate.onItemChanged(arena)
                })
            }
        }
    }

    private fun stopRefreshTimer(arena: FirebaseArena) {
        stopAnimation(arena)
        RefreshTimer.stop(arena.id)
    }

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