package io.stanc.pogotool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup

class RaidMeetupViewModel(raidMeetup: FirebaseRaidMeetup?): ViewModel() {
    private val TAG = javaClass.name

    var raidMeetup: FirebaseRaidMeetup? = raidMeetup
        private set

    val isRaidMeetupAnnounced = ObservableField<Boolean>()
    val meetupTime = ObservableField<String>()
    val numParticipants = ObservableField<String>()
    val participants = ObservableField<List<String>>()
    val isUserParticipate = ObservableField<Boolean>()

    init {
        updateData(raidMeetup)
    }

    fun updateData(raidMeetup: FirebaseRaidMeetup?) {
        this.raidMeetup = raidMeetup

        raidMeetup?.let {
            changeMeetupData(raidMeetup)
        } ?: kotlin.run {
            resetMeetupData()
        }
//        Log.d(TAG, "Debug:: updateData(), isRaidMeetupAnnounced: ${isRaidMeetupAnnounced.get()}, meetupTime: ${meetupTime.get()}, isUserParticipate: ${isUserParticipate.get()}")
    }

    private fun changeMeetupData(raidMeetup: FirebaseRaidMeetup) {
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
}