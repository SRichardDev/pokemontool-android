package io.stanc.pogoradar.viewmodel.arena

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup

class RaidMeetupViewModel: ViewModel() {
    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    var raidMeetup: FirebaseRaidMeetup? = null
        private set

    val isRaidMeetupAnnounced = MutableLiveData<Boolean>(false)
    val meetupTime = MutableLiveData<String>(App.geString(R.string.arena_raid_meetup_time_none))
    val numParticipants = MutableLiveData<String>("0")
    val participants = MutableLiveData<List<FirebasePublicUser>>(emptyList())
    val isUserParticipate = MutableLiveData<Boolean>(false)

    fun updateData(raidMeetup: FirebaseRaidMeetup?) {
        this.raidMeetup = raidMeetup

        raidMeetup?.let {
            changeMeetupData(raidMeetup)
        } ?: run {
            reset()
        }
//        Log.d(TAG, "Debug:: updateData(), numParticipants: ${numParticipants.get()}, participants: ${participants.get()}, isUserParticipate: ${isUserParticipate.get()}")
    }

    private fun changeMeetupData(raidMeetup: FirebaseRaidMeetup) {
        isRaidMeetupAnnounced.value = raidMeetup.meetupTime != DatabaseKeys.DEFAULT_MEETUP_TIME
        Log.i(TAG, "Debug:: changeMeetupData($raidMeetup) isRaidMeetupAnnounced: ${isRaidMeetupAnnounced.value}")
        numParticipants.value = raidMeetup.participantUserIds.size.toString()
        isUserParticipate.value = raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id)
        meetupTime.value = raidMeetup.meetupTime

        updateParticipantsList(raidMeetup)
    }

    fun reset() {
        isRaidMeetupAnnounced.value = false
        Log.i(TAG, "Debug:: reset() isRaidMeetupAnnounced: $isRaidMeetupAnnounced")
        numParticipants.value = "0"
        participants.value = emptyList()
        isUserParticipate.value = false
        meetupTime.value = App.geString(R.string.arena_raid_meetup_time_none)
    }

    private fun updateParticipantsList(raidMeetup: FirebaseRaidMeetup) {

        removeParticipantsIfNecessary(raidMeetup)
        addParticipantsIfNecessary(raidMeetup)
    }

    private fun removeParticipantsIfNecessary(raidMeetup: FirebaseRaidMeetup) {

        var publicUsers = mutableListOf<FirebasePublicUser>()

        participants.value?.let { participants ->

            publicUsers = participants.toMutableList()

            participants.forEach { publicUser ->

                if (!raidMeetup.participantUserIds.contains(publicUser.id)) {
                    publicUsers.find { it.id == publicUser.id }?.let {
                        publicUsers.remove(it)
                    }
                }
            }
        }
        participants.value = publicUsers
    }

    private fun addParticipantsIfNecessary(raidMeetup: FirebaseRaidMeetup) {

        raidMeetup.participantUserIds.forEach {  userId ->

            firebase.loadPublicUser(userId, onCompletionCallback = { publicUser ->

                var publicUsers = mutableListOf<FirebasePublicUser>()

                participants.value?.let { participants ->

                    publicUsers = participants.toMutableList()

                    if (!participants.contains(publicUser)) {
                        publicUsers.add(publicUser)
                    }
                }

                participants.value = publicUsers
            })
        }
    }
}