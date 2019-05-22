package io.stanc.pogotool.viewmodels

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebaseRaid
import io.stanc.pogotool.firebase.node.FirebaseRaid.RaidState
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup
import io.stanc.pogotool.firebase.node.FirebaseRaidbossDefinition

class RaidViewModel(private var arena: FirebaseArena): ViewModel() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()
    private var raidMeetup: FirebaseRaidMeetup? = null
//    private var publicUser: List<FirebasePublicUser>? = null

    private val raidMeetupObserver = object: FirebaseDatabase.Observer<FirebaseRaidMeetup> {

        override fun onItemChanged(item: FirebaseRaidMeetup) {
            Log.i(TAG, "Debug:: onItemChanged($item)")
            raidMeetup = item
            updateMeetupData(item)
        }

        override fun onItemRemoved(itemId: String) {
            raidMeetup = null
            resetMeetupData()

        }
    }

    /**
     * observable fields
     */

    val isRaidAnnounced = ObservableField<Boolean>(false)
    val isRaidMeetupAnnounced = ObservableField<Boolean>(false)
    val raidState = ObservableField<RaidState>(RaidState.NONE)
    // TODO: update raidTime ? maybe server should change: egg -> raid -> expired
    val raidTime = ObservableField<String>(App.geString(R.string.arena_raid_time_none))
    val meetupTime = ObservableField<String>(App.geString(R.string.arena_raid_meetup_time_none))
    val numParticipants = ObservableField<String>("0")
    private val participants = ObservableField<List<String>>(emptyList())
    val isUserParticipate = ObservableField<Boolean>(false)
    val isRaidBossMissing = ObservableField<Boolean>(false) // isRaidAnnounced && raidState == RaidState.RAID_RUNNING && arena.raid?.raidBossId == null


    init {
        updateData(arena)
    }

    override fun onCleared() {
        Log.i(TAG, "Debug:: onCleared()")
        raidMeetup?.id?.let { firebase.removeObserver(raidMeetupObserver, it) }
        super.onCleared()
    }

    /**
     * interface
     */

    fun updateData(arena: FirebaseArena) {
        Log.i(TAG, "Debug:: updateData($arena)")

        requestRaidMeetupData(arena)

        arena.raid?.let { raid ->
            isRaidAnnounced.set(raid.currentRaidState() != FirebaseRaid.RaidState.NONE)
            raidState.set(raid.currentRaidState())
            raidTime.set(raidTime(raid))
            isRaidBossMissing.set(isRaidAnnounced.get() == true && raidState.get() == RaidState.RAID_RUNNING && raid.raidBossId == null)

        } ?: kotlin.run {
            isRaidAnnounced.set(false)
            raidState.set(FirebaseRaid.RaidState.NONE)
            raidTime.set(App.geString(R.string.arena_raid_time_none))
            isRaidBossMissing.set(false)
        }

        this.arena = arena

        Log.i(TAG, "Debug:: updateData(), isRaidAnnounced: ${isRaidAnnounced.get()}, raidState: ${raidState.get()?.name}, raidTime: ${raidTime.get()}")
    }

    fun changeParticipation(participate: Boolean) {

        raidMeetup?.id?.let {

            if (participate) {
                firebase.pushRaidMeetupParticipation(it)
            } else {
                firebase.cancelRaidMeetupParticipation(it)
            }

        } ?: kotlin.run {
            Log.e(TAG, "could not changed participation($participate), because: raidMeetup?.id: ${raidMeetup?.id}")
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

        raidMeetup?.id?.let { firebase.removeObserver(raidMeetupObserver, it) }
        Log.d(TAG, "Debug:: requestRaidMeetupData(arena: $arena), arena.raid?.raidMeetupId?: ${arena.raid?.raidMeetupId}")

        arena.raid?.raidMeetupId?.let {
            firebase.addObserver(raidMeetupObserver, it)
        } ?: kotlin.run {
            resetMeetupData()
        }
    }

    private fun updateMeetupData(raidMeetup: FirebaseRaidMeetup) {
        Log.d(TAG, "Debug:: updateMeetupData(raidMeetup: $raidMeetup)")
        isRaidMeetupAnnounced.set(true)
        numParticipants.set(raidMeetup.participantUserIds.size.toString())
        participants.set(raidMeetup.participantUserIds)
        isUserParticipate.set(raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id))
        meetupTime.set(raidMeetup.meetupTime)
    }

    private fun resetMeetupData() {
        isRaidMeetupAnnounced.set(false)
        numParticipants.set("0")
        participants.set(emptyList())
        isUserParticipate.set(false)
        meetupTime.set(App.geString(R.string.arena_raid_meetup_time_none))
    }

    private fun requestParticipants() {
//        raidMeetup?.numParticipants.... => this.publicUser
    }

    private fun raidTime(raid: FirebaseRaid): String {


        val time =  when(raid.currentRaidState()) {
            RaidState.EGG_HATCHES -> {
                raid.timeEggHatches()
            }
            RaidState.RAID_RUNNING -> {
                raid.timeRaidEnds()
            }

            else -> null
        } ?: kotlin.run { "00:00" }

        Log.i(TAG, "Debug:: raidTime($raid) state: ${raid.currentRaidState().name}, time: $time")

        return time
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