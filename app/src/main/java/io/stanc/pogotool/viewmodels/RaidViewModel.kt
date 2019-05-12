package io.stanc.pogotool.viewmodels

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebaseRaid

class RaidViewModel(private var arena: FirebaseArena): ViewModel() {

    private val TAG = javaClass.name

    /**
     * observable fields
     */

    val isRaidAnnounced = ObservableField<Boolean>(false)
    val raidState = ObservableField<FirebaseRaid.RaidState>(FirebaseRaid.RaidState.NONE)

    init {
        updateData(arena)
    }

    /**
     * private handler
     */

    override fun onCleared() {
        Log.i(TAG, "Debug:: onCleared()")
//        FirebaseServer.removeNodeEventListener("${arena.databasePath()}/${arena.id}/$DATABASE_ARENA_RAID", raidChangeListener)
        super.onCleared()
    }

    /**
     * methods
     */

    fun updateData(arena: FirebaseArena) {

        arena.raid?.let {
            isRaidAnnounced.set(it.currentRaidState() != FirebaseRaid.RaidState.NONE)
            raidState.set(it.currentRaidState())
        } ?: kotlin.run {
            isRaidAnnounced.set(false)
            raidState.set(FirebaseRaid.RaidState.NONE)
        }
    }

    /**
     * observer
     */

//    interface RaidViewModelObserver {
//        fun raidChanged(raid: FirebaseRaid?)
//    }
//
//    fun addObserver(observer: RaidViewModelObserver) {
//        observerManager.addObserver(observer)
//        observer.raidChanged(arena.raid)
//        Log.d(TAG, "Debug:: addObserver($observer) observers: ${observerManager.observers()}")
//    }
//
//    fun removeObserver(observer: RaidViewModelObserver) {
//        observerManager.removeObserver(observer)
//    }
}