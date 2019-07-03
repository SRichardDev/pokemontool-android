package io.stanc.pogotool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebasePublicUser
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup

class RaidMeetupViewModel(raidMeetup: FirebaseRaidMeetup?): ViewModel() {
    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    var raidMeetup: FirebaseRaidMeetup? = raidMeetup
        private set

    val isRaidMeetupAnnounced = ObservableField<Boolean>()
    val meetupTime = ObservableField<String>()
    val numParticipants = ObservableField<String>()
    val participants = ObservableField<List<FirebasePublicUser>>()
    val isUserParticipate = ObservableField<Boolean>()

    init {
        updateData(raidMeetup)
    }

    fun updateData(raidMeetup: FirebaseRaidMeetup?) {
        this.raidMeetup = raidMeetup

        raidMeetup?.let {
            changeMeetupData(raidMeetup)
        } ?: run {
            resetMeetupData()
        }
//        Log.d(TAG, "Debug:: updateData(), numParticipants: ${numParticipants.get()}, participants: ${participants.get()}, isUserParticipate: ${isUserParticipate.get()}")
    }

    private fun changeMeetupData(raidMeetup: FirebaseRaidMeetup) {
        isRaidMeetupAnnounced.set(true)
        numParticipants.set(raidMeetup.participantUserIds.size.toString())
        isUserParticipate.set(raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id))
        meetupTime.set(raidMeetup.meetupTime)

        updateParticipantsList(raidMeetup)
    }

    private fun resetMeetupData() {
        isRaidMeetupAnnounced.set(false)
        numParticipants.set("0")
        participants.set(emptyList())
        isUserParticipate.set(false)
        meetupTime.set(App.geString(R.string.arena_raid_meetup_time_none))
    }

    private fun updateParticipantsList(raidMeetup: FirebaseRaidMeetup) {

        removeParticipantsIfNecessary(raidMeetup)
        addParticipantsIfNecessary(raidMeetup)
    }

    private fun removeParticipantsIfNecessary(raidMeetup: FirebaseRaidMeetup) {

        var publicUsers = mutableListOf<FirebasePublicUser>()

        participants.get()?.let { participants ->

            publicUsers = participants.toMutableList()

            participants.forEach { publicUser ->

                if (!raidMeetup.participantUserIds.contains(publicUser.id)) {
                    publicUsers.find { it.id == publicUser.id }?.let {
                        publicUsers.remove(it)
                    }
                }
            }
        }
        participants.set(publicUsers)
    }

    private fun addParticipantsIfNecessary(raidMeetup: FirebaseRaidMeetup) {

        raidMeetup.participantUserIds.forEach {  userId ->

            firebase.loadPublicUser(userId, onCompletionCallback = { publicUser ->

                var publicUsers = mutableListOf<FirebasePublicUser>()

                participants.get()?.let { participants ->

                    publicUsers = participants.toMutableList()

                    if (!participants.contains(publicUser)) {
                        publicUsers.add(publicUser)
                    }
                }

                participants.set(publicUsers)
            })
        }
    }

}