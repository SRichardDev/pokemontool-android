package io.stanc.pogotool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseNodeObserverManager
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup
import io.stanc.pogotool.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogotool.utils.Observables.dependantObservableField
import io.stanc.pogotool.viewmodel.RaidStateViewModel.RaidState

class RaidViewModel(private var arena: FirebaseArena): ViewModel() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private var raidStateViewModel: RaidStateViewModel = RaidStateViewModel(arena.raid)
    private var raidMeetupViewModel: RaidMeetupViewModel = RaidMeetupViewModel(null)
//    private var publicUser: List<FirebasePublicUser>? = null

    private val raidMeetupObserver = object: FirebaseNodeObserverManager.Observer<FirebaseRaidMeetup> {

        override fun onItemChanged(item: FirebaseRaidMeetup) {
            raidMeetupViewModel.updateData(item)
        }

        override fun onItemRemoved(itemId: String) {
            raidMeetupViewModel.updateData(null)
        }
    }

    /**
     * observable fields
     */


    val isRaidBossMissing = ObservableField<Boolean>(false) // isRaidAnnounced && raidState == RaidState.RAID_RUNNING && arena.raid?.raidBossId == null

    // RaidStateViewModel
    val raidState = dependantObservableField(raidStateViewModel.raidState) {
        raidStateViewModel.raidState.get()
    }
    val raidTime = dependantObservableField(raidStateViewModel.raidTime) {
        raidStateViewModel.raidTime.get()
    }
    val isRaidAnnounced = dependantObservableField(raidStateViewModel.isRaidAnnounced) {
        raidStateViewModel.isRaidAnnounced.get()
    }

    // RaidMeetupViewModel
    val isRaidMeetupAnnounced = dependantObservableField(raidMeetupViewModel.isRaidMeetupAnnounced) {
        raidMeetupViewModel.isRaidMeetupAnnounced.get()
    }
    val meetupTime = dependantObservableField(raidMeetupViewModel.meetupTime) {
        raidMeetupViewModel.meetupTime.get()
    }
    val numParticipants = dependantObservableField(raidMeetupViewModel.numParticipants) {
        raidMeetupViewModel.numParticipants.get()
    }
    val participants = dependantObservableField(raidMeetupViewModel.participants) {
        raidMeetupViewModel.participants.get()
    }
    val isUserParticipate = dependantObservableField(raidMeetupViewModel.isUserParticipate) {
        raidMeetupViewModel.isUserParticipate.get()
    }

    init {
        updateData(arena)
    }

    override fun onCleared() {
        raidMeetupViewModel.raidMeetup?.let { firebase.removeObserver(raidMeetupObserver, it) }
        super.onCleared()
    }

    /**
     * interface
     */

    fun updateData(arena: FirebaseArena) {
//        Log.i(TAG, "Debug:: updateData($arena)")

        requestRaidMeetupData(arena)

        raidStateViewModel.updateData(arena.raid)

        arena.raid?.let { raid ->

            isRaidBossMissing.set(isRaidAnnounced.get() == true && raidState.get() == RaidState.RAID_RUNNING && raid.raidBossId == null)

        } ?: kotlin.run {

            isRaidBossMissing.set(false)
        }

        this.arena = arena

//        Log.i(TAG, "Debug:: updateData(), isRaidAnnounced: ${isRaidAnnounced.get()}, raidState: ${raidState.get()?.name}, raidTime: ${raidTime.get()}")
    }

    fun changeParticipation(participate: Boolean) {

        raidMeetupViewModel.raidMeetup?.id?.let {

            if (participate) {
                firebase.pushRaidMeetupParticipation(it)
            } else {
                firebase.cancelRaidMeetupParticipation(it)
            }

        } ?: kotlin.run {
            Log.e(TAG, "could not changed participation($participate), because: raidMeetup?.id: ${raidMeetupViewModel.raidMeetup?.id}")
        }
    }

    fun createMeetup(meetupTime: String) {

        arena.raid?.let { raid ->
            val raidMeetup = FirebaseRaidMeetup("", meetupTime, participantUserIds = emptyList(), chat = emptyList())
            firebase.pushRaidMeetup(raid.databasePath(), raidMeetup)

        } ?: kotlin.run {
            Log.e(TAG, "Could not push raid meetup, because raid is null of arena: $arena")
        }
    }

    fun sendRaidBoss(raidBoss: FirebaseRaidbossDefinition) {

        arena.raid?.let { raid ->
            firebase.pushRaidBoss(raid.databasePath(), raidBoss)
        }
    }

    /**
     * private
     */

    private fun requestRaidMeetupData(arena: FirebaseArena) {

        raidMeetupViewModel.raidMeetup?.let { firebase.removeObserver(raidMeetupObserver, it) }
        Log.d(TAG, "Debug:: requestRaidMeetupData(arena: $arena), arena.raid?.raidMeetupId?: ${arena.raid?.raidMeetupId}")

        arena.raid?.raidMeetupId?.let { id ->

            val raidMeetup = FirebaseRaidMeetup(id, "", emptyList(), emptyList())
            firebase.addObserver(raidMeetupObserver, raidMeetup)

        } ?: kotlin.run {
            raidMeetupViewModel.updateData(null)
        }
    }



    private fun requestParticipants() {
//        raidMeetup?.numParticipants.... => this.publicUser
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