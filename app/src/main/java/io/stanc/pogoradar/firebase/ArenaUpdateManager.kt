package io.stanc.pogoradar.firebase

import android.util.Log
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.utils.RefreshTimer
import io.stanc.pogoradar.viewmodel.arena.currentRaidState


object ArenaUpdateManager {
    private val TAG = javaClass.name

    private val arenas = mutableListOf<FirebaseArena>()

    private val arenaObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebaseArena.new(dataSnapshot)
    })

    fun addObserver(observer: FirebaseNodeObserver<FirebaseArena>, arena: FirebaseArena) {
        Log.d(TAG, "Debug:: addObserver(observer: $observer, arena: $arena)")

        if (arenas.firstOrNull { it.id == arena.id } == null) {
            Log.d(TAG, "Debug:: addObserver() arena is new")
            arenas.add(arena)
        }

        arenaObserverManager.addObserver(observer, arena)
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebaseArena>, arenaId: String) {
        Log.d(TAG, "Debug:: removeObserver(observer: $observer, arenaId: $arenaId), arenas: $arenas")

        arenas.firstOrNull { it.id == arenaId }?.let { arenaToRemove ->

            Log.v(TAG, "Debug:: arenaObserverManager.removeObserver")
            removeObserver(observer, arenaToRemove)

        } ?: run {
            Log.w(TAG, "could not remove observer $observer for arena with id: $arenaId!")
        }
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebaseArena>, arena: FirebaseArena) {
        arenaObserverManager.removeObserver(observer, arena)

        if (arenaObserverManager.observers(arena.id).isEmpty()) {
            arenas.removeIf { it.id == arena.id }
        }
    }

    fun start() {
        startRefreshTimer()
    }

    fun stop() {
        arenaObserverManager.clear()
        arenas.clear()
        stopRefreshTimer()
    }

    /**
     * update
     */

    private const val REFRESH_TIMER_ID = "arenaRefreshTimerId"

    private fun startRefreshTimer() {
        Log.d(TAG, "Debug:: startRefreshTimer(minutes: 1, id: \"arenaRefreshTimer\")")
        RefreshTimer.run(minutes = 1, id = REFRESH_TIMER_ID, onFinished = {
            Log.d(TAG, "Debug:: RefreshTimer.onFinished")
            updateArenasRaidState()
            startRefreshTimer()
        })
    }

    private fun stopRefreshTimer() {
        Log.d(TAG, "Debug:: stopRefreshTimer(id: \"arenaRefreshTimer\")")
        RefreshTimer.stop(REFRESH_TIMER_ID)
    }

    private fun updateArenasRaidState() {
        Log.d(TAG, "Debug:: updateArenasRaidState() arenas: $arenas")

        arenas.forEach { arena ->

            val latestRaidState = arena.raid?.latestRaidState
            arena.raid?.latestRaidState = currentRaidState(arena.raid)
            val currentRaidState = arena.raid?.latestRaidState

            Log.v(TAG, "Debug:: updateArenasRaidState(arena: $arena) latestRaidState: ${latestRaidState?.name}, currentRaidState: ${currentRaidState?.name}")

            if (latestRaidState != currentRaidState) {
                updateObserversAboutArenaDidChange(arena)
            }
        }
    }

    private fun updateObserversAboutArenaDidChange(arena: FirebaseArena) {
        Log.d(TAG, "Debug:: updateObserversAboutArenaDidChange(arena: $arena)")
        arenaObserverManager.observers(arena.id).forEach { observer ->
            Log.d(TAG, "Debug:: updateObserversAboutArenaDidChange() for observer: $observer")
            observer?.onItemChanged(arena)
        }
    }
}