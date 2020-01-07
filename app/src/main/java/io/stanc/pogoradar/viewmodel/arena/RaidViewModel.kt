package io.stanc.pogoradar.viewmodel.arena

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.firebase.DatabaseKeys.DEFAULT_TIME
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP_NONE
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.utils.TimeCalculator
import kotlin.math.min

class RaidViewModel: ViewModel() {
    private val TAG = javaClass.name

    private var arena: FirebaseArena? = null
    var raidMeetup: FirebaseRaidMeetup? = null
        private set
    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private val raidStateViewModel = RaidStateViewModel()

    /**
     * observable fields
     */

    val isRaidBossMissing = MutableLiveData<Boolean>(false)
    val raidImage = MutableLiveData<Drawable?>()
    val isChangingMeetupTime = MutableLiveData<Boolean>(false)

    val isRaidMeetupAnnounced = MutableLiveData<Boolean>(false)
    val meetupTime = MutableLiveData<String>(DEFAULT_TIME)
    val numParticipants = MutableLiveData<String>("0")
    val participants = MutableLiveData<List<FirebasePublicUser>>(emptyList())
    val isUserParticipate = MutableLiveData<Boolean>(false)

    // RaidStateViewModel

    val raidState: MutableLiveData<RaidState> = raidStateViewModel.raidState
    val raidTime: MutableLiveData<String?> = raidStateViewModel.raidTime
    val isRaidAnnounced: MutableLiveData<Boolean> = raidStateViewModel.isRaidAnnounced

    /**
     * interface
     */

    fun participant(id: String): FirebasePublicUser? {
        return participants.value?.firstOrNull { it.id == id }
    }

    fun updateData(arena: FirebaseArena, context: Context) {
        Log.w(TAG, "updateData(arena: $arena)")
        this.arena = arena

        arena.raid?.let { raidStateViewModel.updateData(it) }
        raidImage.value = imageDrawable(arena, context)

        isRaidBossMissing.value = arena.raid?.let { raid ->
            raidState.value == RaidState.RAID_RUNNING && raid.raidBoss == null
        } ?: run {
            false
        }

        // raid meetup
        arena.raid?.raidMeetup?.let {
            this.raidMeetup = it
            changeMeetupData(it)
        } ?: run {
            this.raidMeetup = null
            resetMeetup()
        }
//        Log.d(TAG, "Debug:: updateData(), numParticipants: ${numParticipants.get()}, participants: ${participants.get()}, isUserParticipate: ${isUserParticipate.get()}")
    }

    private fun changeMeetupData(raidMeetup: FirebaseRaidMeetup) {
        isRaidMeetupAnnounced.value = raidMeetup.meetupTimestamp != TIMESTAMP_NONE
        numParticipants.value = raidMeetup.participantUserIds.size.toString()
        isUserParticipate.value = raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id)
        meetupTime.value = if(raidMeetup.meetupTimestamp != TIMESTAMP_NONE) TimeCalculator.format(raidMeetup.meetupTimestamp) else DEFAULT_TIME

        updateParticipantsList(raidMeetup)
    }

    fun reset() {
        isRaidBossMissing.value = false
        raidImage.value = null
        isChangingMeetupTime.value = false

        resetMeetup()

        raidStateViewModel.reset()
    }

    private fun resetMeetup() {
        isRaidMeetupAnnounced.value = false
        numParticipants.value = "0"
        participants.value = emptyList()
        isUserParticipate.value = false
        meetupTime.value = DEFAULT_TIME
    }

    fun changeParticipation(participate: Boolean) {

        arena?.raid?.raidMeetup?.let {

            if (participate) {
                firebase.pushRaidMeetupParticipation()
            } else {
                firebase.cancelRaidMeetupParticipation()
            }

        } ?: run {
            Log.e(TAG, "could not changed participation($participate), because: raidMeetup?.id: ${raidMeetup?.id}")
        }
    }

    fun wantToChangeMeetupTime() {
        isChangingMeetupTime.value = true
    }

    fun changeMeetupTime(hour: Int, minutes: Int) {
        isChangingMeetupTime.value = false

        arena?.raid?.raidMeetup?.let { raidMeetup ->

            TimeCalculator.timestampOfToday(hour, minutes)?.let {  meetupTimestamp ->
                firebase.changeRaidMeetupTime(raidMeetup, meetupTimestamp)
            } ?: run {
                Log.e(TAG, "Could not change raid meetup time, because timestamp creation failed for hour: $hour and minutes: $minutes!")
            }

        } ?: run {
            Log.e(TAG, "Could not change raid meetup time, because arena: $arena, raid: ${arena?.raid}, or raidMeetup: ${arena?.raid?.raidMeetup} is null!")
        }
    }

    fun sendRaidBoss(raidBoss: FirebaseRaidbossDefinition) {

        arena?.raid?.let { raid ->
            firebase.pushRaidBoss(raid.databasePath(), raidBoss)
        }
    }

    /**
     * private
     */

    private fun imageDrawable(arena: FirebaseArena, context: Context): Drawable? {
        return FirebaseImageMapper.raidDrawable(context, arena)
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