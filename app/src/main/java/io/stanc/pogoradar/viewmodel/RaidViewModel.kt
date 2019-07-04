package io.stanc.pogoradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.utils.Observables.dependantObservableField
import io.stanc.pogoradar.viewmodel.RaidStateViewModel.RaidState

class RaidViewModel(private var arena: FirebaseArena): ViewModel() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private var raidStateViewModel: RaidStateViewModel = RaidStateViewModel(arena.raid)
    var raidMeetupViewModel: RaidMeetupViewModel = RaidMeetupViewModel(null)

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

        } ?: run {

            isRaidBossMissing.set(false)
        }

        this.arena = arena

//        Log.i(TAG, "Debug:: updateData(), isRaidBossMissing: ${isRaidBossMissing.get()}, isRaidAnnounced: ${isRaidAnnounced.get()}, raidState: ${raidState.get()?.name}, raidTime: ${raidTime.get()}")
    }

    fun changeParticipation(participate: Boolean) {

        raidMeetupViewModel.raidMeetup?.id?.let {

            if (participate) {
                firebase.pushRaidMeetupParticipation(it)
            } else {
                firebase.cancelRaidMeetupParticipation(it)
            }

        } ?: run {
            Log.e(TAG, "could not changed participation($participate), because: raidMeetup?.id: ${raidMeetupViewModel.raidMeetup?.id}")
        }
    }

    fun createMeetup(meetupTime: String) {

        arena.raid?.let { raid ->

            val raidMeetup = FirebaseRaidMeetup("", meetupTime, participantUserIds = emptyList(), chat = emptyList())
            firebase.pushRaidMeetup(raid.databasePath(), raidMeetup)

        } ?: run {
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

        arena.raid?.raidMeetupId?.let { id ->

            val raidMeetup = FirebaseRaidMeetup(id, "", emptyList(), emptyList())
            firebase.addObserver(raidMeetupObserver, raidMeetup)

        } ?: run {
            raidMeetupViewModel.updateData(null)
        }
    }
}