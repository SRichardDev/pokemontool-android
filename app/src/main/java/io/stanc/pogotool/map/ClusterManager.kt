package io.stanc.pogotool.map

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
//import com.arsy.maps_library.MapRipple
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.utils.DelayedTrigger
import java.lang.ref.WeakReference
import io.stanc.pogotool.utils.Kotlin
import io.stanc.pogotool.utils.RefreshTimer
import io.stanc.pogotool.utils.TimeCalculator
import io.stanc.pogotool.viewmodel.RaidStateViewModel


class ClusterManager(context: Context, googleMap: GoogleMap, private val delegate: MarkerDelegate) {

    private val TAG = javaClass.name

    private val pokestopClusterManager: ClusterManager<ClusterPokestop> = ClusterManager(context, googleMap)
    private val arenaClusterManager: ClusterManager<ClusterArena> = ClusterManager(context, googleMap)

    private val context = WeakReference(context)
    private val map = WeakReference(googleMap)
    
    private val pokestopClustering = DelayedTrigger(200) { pokestopClusterManager.cluster() }
    private val arenaClustering = DelayedTrigger(200) { arenaClusterManager.cluster() }
    // https://github.com/aarsy/GoogleMapsAnimations
//    private val arenaRippleAnimations = HashMap<Any, WeakReference<MapRipple>>()
    private val arenaCircles = HashMap<Any, Circle>()

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

            val viewModel = RaidStateViewModel(raid)

            if (viewModel.isRaidAnnounced.get() == true) {

                Kotlin.safeLet(viewModel.raidTime.get(), (raid.timestamp as? Long)) { raidTime, timestamp ->

                    startRefreshTimer(arena, raidTime, timestamp)

                } ?: kotlin.run {
                    Log.e(TAG,"Debug:: viewModel.raidTime: ${viewModel.raidTime.get()}, raid.timestamp: ${raid.timestamp} for $raid")
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
        map.get()?.let {

            stopAnimation(arena)

            val circleOptions = CircleOptions().apply {
                center(arena.geoHash.toLatLng())
                radius(100.0)
                strokeColor(Color.TRANSPARENT)
                fillColor(R.color.redTransparent)
            }

            arenaCircles[arena.id] = it.addCircle(circleOptions)
        }

        // TODO: search for performant googlemap pulse animation
//        Kotlin.safeLet(map.get(), context.get()) { map, context ->
//
//            stopAnimation(arena)
//
//            val mapRipple = mapRipple(map, arena, context)
//            arenaRippleAnimations[arena.id] = WeakReference(mapRipple)
//            mapRipple.startRippleMapAnimation()
//
//        } ?: kotlin.run {
//            Log.e(TAG, "could not start map animation, because map.get(): ${map.get()} or context.get(): ${context.get()}")
//        }
    }

    private fun stopAnimation(arena: FirebaseArena) {
        arenaCircles.remove(arena.id)?.remove()
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