package io.stanc.pogotool.viewmodels

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebaseRaid
import io.stanc.pogotool.firebase.node.FirebaseRaid.RaidState
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup

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
    }

    /**
     * observable fields
     */

    val isRaidAnnounced = ObservableField<Boolean>(false)
    val isRaidMeetupAnnounced = ObservableField<Boolean>(false)
    val raidState = ObservableField<FirebaseRaid.RaidState>(FirebaseRaid.RaidState.NONE)
    // TODO: update time ? maybe server should change: egg -> raid -> expired
    val time = ObservableField<String>("-00:00")
    val numParticipants = ObservableField<String>("0")
    private val participants = ObservableField<List<String>>(emptyList())
    val isUserParticipate = ObservableField<Boolean>(false)

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
            time.set(raidTime(raid))

        } ?: kotlin.run {
            isRaidAnnounced.set(false)
            raidState.set(FirebaseRaid.RaidState.NONE)
            time.set("-00:00")
        }

        this.arena = arena

        Log.i(TAG, "Debug:: updateData(), isRaidAnnounced: ${isRaidAnnounced.get()}, raidState: ${raidState.get()?.name}, time: ${time.get()}")
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
        val raidMeetup = FirebaseRaidMeetup("", meetupTime, participantUserIds = emptyList(), chat = emptyList())
        firebase.pushRaidMeetup(raidMeetup, FirebaseUser.userData?.id)
    }


//    fun getNumParticipants(): List<String>? {
//        return publicUser....
//    }


//    private fun getMeetupIfUserParticipates(): FirebaseRaidMeetup? {
//
//        return if (isUserParticipating) {
//
//            FirebaseUser.userData?.id?.let {
//
//                val numParticipants: List<String> = listOf(it)
//                FirebaseRaidMeetup("",  formattedTime(meetupTimeHour, meetupTimeMinutes), numParticipants)
//
//            } ?: kotlin.run {
//                Log.e(TAG, "could not send raid meetup, because user is logged out. (userData: ${FirebaseUser.userData}, userData?.id: ${FirebaseUser.userData?.id})")
//                null
//            }
//
//        } else {
//            null
//        }
//    }

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
        Log.d(TAG, "Debug:: updateData(raidMeetup: $raidMeetup)")
        isRaidMeetupAnnounced.set(true)
        numParticipants.set(raidMeetup.participantUserIds.size.toString())
        participants.set(raidMeetup.participantUserIds)
        isUserParticipate.set(raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id))
    }

    private fun resetMeetupData() {
        isRaidMeetupAnnounced.set(false)
        numParticipants.set("0")
        participants.set(emptyList())
        isUserParticipate.set(false)
    }

    private fun requestParticipants() {
//        raidMeetup?.numParticipants.... => this.publicUser
    }

    private fun raidTime(raid: FirebaseRaid): String {

        return when(raid.currentRaidState()) {
            RaidState.EGG_HATCHES -> {
                raid.timeEggHatches()
            }
            RaidState.RAID_RUNNING -> {
                raid.timeRaidEnds()
            }

            else -> null
        } ?: kotlin.run { "00:00" }
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