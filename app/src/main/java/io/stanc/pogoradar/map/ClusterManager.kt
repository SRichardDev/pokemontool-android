package io.stanc.pogoradar.map

//import com.arsy.maps_library.MapRipple
import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import io.stanc.pogoradar.MapFilterSettings
import io.stanc.pogoradar.firebase.FirebaseNodeObserver
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.DelayedTrigger
import io.stanc.pogoradar.utils.RefreshTimer
import io.stanc.pogoradar.viewmodel.arena.currentRaidState
import io.stanc.pogoradar.viewmodel.arena.isArenaVisibleOnMap
import io.stanc.pogoradar.viewmodel.pokestop.PokestopViewModel
import java.lang.ref.WeakReference


class ClusterManager(context: Context, googleMap: GoogleMap, private val delegate: MarkerDelegate) {

    private val TAG = javaClass.name

    private val pokestopClusterManager: ClusterManager<ClusterPokestop> = ClusterManager(context, googleMap)
    private val arenaClusterManager: ClusterManager<ClusterArena> = ClusterManager(context, googleMap)

    private val clusterPokestops: HashMap<String, WeakReference<ClusterPokestop>> = HashMap()
    private val clusterArenas: HashMap<String, WeakReference<ClusterArena>> = HashMap()
    
    private val pokestopClustering = DelayedTrigger(200) { pokestopClusterManager.cluster() }
    private val arenaClustering = DelayedTrigger(200) { arenaClusterManager.cluster() }

    private val arenaObserver = object: FirebaseNodeObserver<FirebaseArena> {

        override fun onItemChanged(item: FirebaseArena) {
            Log.d(TAG, "Debug:: onItemChanged($item)")
            removeArena(item.id)
            addArena(item)
        }

        override fun onItemRemoved(itemId: String) {
            Log.w(TAG, "Debug:: onItemRemoved(arenaId: $itemId)")
            removeArena(itemId)
        }
    }

//    private val pokestopObserver = object: FirebaseNodeObserver<FirebasePokestop> {
//
//        override fun onItemChanged(item: FirebasePokestop) {
//            dataObject = item
//        }
//
//        override fun onItemRemoved(itemId: String) {
//            dataObject = null
//        }
//    }

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
            Log.d(TAG, "Debug:: onInfoWindowClicked(arena: ${(marker.tag as? FirebaseArena)?.name})")
            delegate.onArenaInfoWindowClicked(it)
        }

        (tag as? FirebasePokestop)?.let {
            Log.d(TAG, "Debug:: onInfoWindowClicked(pokestop: ${(marker.tag as? FirebasePokestop)?.name})")
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

    fun showPokestopsAndRemoveOldOnes(pokestops: List<FirebasePokestop>) {
        Log.d(TAG, "Debug:: showPokestopsAndRemoveOldOnes(${pokestops.map { it.name }})")

        val oldPokestopIds = clusterPokestops.keys.minus(pokestops.map { it.id })

        // add new ones
        pokestops.forEach { addObserver(it) }

        // remove old ones
//        oldPokestopIds.forEach { removeObserver(it) }

        pokestopClustering.trigger()
    }

    fun showPokestops(pokestops: List<FirebasePokestop>) {
        Log.d(TAG, "Debug:: showPokestops(${pokestops.map { it.name }})")

        pokestops.forEach { addObserver(it) }
        pokestopClustering.trigger()
    }

    private fun addObserver(pokestop: FirebasePokestop) {
//        database.addObserver(pokestopObserver, pokestop)
    }

    private fun removeObserver(pokestop: FirebasePokestop) {
//        database.removeObserver(pokestopObserver, pokestop)
    }

    private fun addPokestop(pokestop: FirebasePokestop) {
        if (!clusterPokestops.containsKey(pokestop.id)) {
            val clusterItem = ClusterPokestop.new(pokestop)
            pokestopClusterManager.addItem(clusterItem)
            clusterPokestops[clusterItem.pokestop.id] = WeakReference(clusterItem)
        }
    }

    private fun removePokestop(pokestopId: String) {
        clusterPokestops.remove(pokestopId)?.get()?.let { clusterPokestop ->
            pokestopClusterManager.removeItem(clusterPokestop)
        }
    }

    fun removeAllPokestops() {
        pokestopClusterManager.clearItems()
        clusterPokestops.clear()
        pokestopClustering.trigger()
    }

    /**
     * Arenas
     */

    private val arenaObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebaseArena.new(dataSnapshot)
    })

    fun showArenasAndRemoveOldOnes(arenas: List<FirebaseArena>) {
        Log.d(TAG, "Debug:: showArenasAndRemoveOldOnes(${arenas.map { it.name }})")

        val oldArenaIds = clusterArenas.keys.minus(arenas.map { it.id })
        val oldArenas = clusterArenas.filter { oldArenaIds.contains(it.key) }.mapNotNull { it.value.get() }

        // add new ones
        arenas.forEach { addArenaAndObserver(it) }

        // remove old ones
        oldArenas.forEach { removeArenaAndObserver(it.arena) }
    }

    fun showArenas(arenas: List<FirebaseArena>) {
        Log.d(TAG, "Debug:: showArenas(${arenas.map { it.name }})")
        arenas.forEach { addArenaAndObserver(it) }
    }

    private fun addArenaAndObserver(arena: FirebaseArena) {
        addArena(arena)
        arenaObserverManager.addObserver(arenaObserver, arena)
        if (!refreshTimerIsRunning) {
            startRefreshTimer()
        }
    }

    private fun removeArenaAndObserver(arena: FirebaseArena) {
        removeArena(arena.id)
        arenaObserverManager.removeObserver(arenaObserver, arena)
        if (refreshTimerIsRunning && clusterArenas.size == 0) {
            stopRefreshTimer()
        }
    }

    private fun addArena(arena: FirebaseArena) {
        Log.v(TAG, "Debug:: addArena(arena: $arena), alreadyAdded: ${clusterArenas.containsKey(arena.id)}, raidState: ${arena.raid?.latestRaidState}")
        if (!clusterArenas.containsKey(arena.id)) {
            val clusterItem = ClusterArena.new(arena)
            arenaClusterManager.addItem(clusterItem)
            clusterArenas[clusterItem.arena.id] = WeakReference(clusterItem)
            arenaClustering.trigger()
        }
    }

    private fun removeArena(arenaId: String) {
        clusterArenas.remove(arenaId)?.get()?.let { clusterArena ->
            arenaClusterManager.removeItem(clusterArena)
            arenaClustering.trigger()
        }
    }

    fun removeAllArenas() {
        Log.w(TAG, "Debug:: removeAllArenas()")
        arenaClusterManager.clearItems()
        clusterArenas.clear()
        arenaClustering.trigger()
    }

    /**
     * refresh & animation
     */

    private val REFRESH_TIMER_ID = "arenaRefreshTimerId"
    private var refreshTimerIsRunning = false

    private fun startRefreshTimer() {
        Log.d(TAG, "Debug:: startRefreshTimer(minutes: 1, id: \"arenaRefreshTimer\")")
        refreshTimerIsRunning = true
        RefreshTimer.run(minutes = 1, id = REFRESH_TIMER_ID, onFinished = {
            Log.d(TAG, "Debug:: RefreshTimer.onFinished")
            updateArenasRaidState()
            startRefreshTimer()
        })
    }

    private fun stopRefreshTimer() {
        Log.d(TAG, "Debug:: stopRefreshTimer(id: \"arenaRefreshTimer\")")
        refreshTimerIsRunning = false
        RefreshTimer.stop(REFRESH_TIMER_ID)
    }

    private fun updateArenasRaidState() {
        Log.d(TAG, "Debug:: updateArenasRaidState() arenas: ${clusterArenas.size}")

        clusterArenas.values.mapNotNull { it.get()?.arena }.forEach { arena ->

            val latestRaidState = arena.raid?.latestRaidState
            arena.raid?.latestRaidState = currentRaidState(arena.raid)
            val currentRaidState = arena.raid?.latestRaidState

            Log.v(TAG, "Debug:: updateArenasRaidState(arena: $arena) latestRaidState: ${latestRaidState?.name}, currentRaidState: ${currentRaidState?.name}")

            if (latestRaidState != currentRaidState) {
                arenaObserver.onItemChanged(arena)
            }
        }
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