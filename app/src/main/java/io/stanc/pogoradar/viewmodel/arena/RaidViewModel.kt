package io.stanc.pogoradar.viewmodel.arena

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.DatabaseKeys.DEFAULT_MEETUP_TIME
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.utils.TimeCalculator

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
    val meetupTime = MutableLiveData<String>(DEFAULT_MEETUP_TIME)
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
        Log.d(TAG, "Debug:: updateData(arena: $arena)")
        this.arena = arena

        arena.raid?.let { raidStateViewModel.updateData(it) }
        raidImage.value = imageDrawable(arena, context)

        isRaidBossMissing.value = arena.raid?.let { raid ->
            raidState.value == RaidState.RAID_RUNNING && raid.raidBossId == null
        } ?: run {
            false
        }
    }

    // TODO: merge with other update method after RaidMeetup was moved to Arena in firebase database
    fun updateData(raidMeetup: FirebaseRaidMeetup) {
        Log.d(TAG, "Debug:: updateData(raidMeetup: $raidMeetup)")
        this.raidMeetup = raidMeetup

        changeMeetupData(raidMeetup)
//        Log.d(TAG, "Debug:: updateData(), numParticipants: ${numParticipants.get()}, participants: ${participants.get()}, isUserParticipate: ${isUserParticipate.get()}")
    }

    private fun changeMeetupData(raidMeetup: FirebaseRaidMeetup) {
        isRaidMeetupAnnounced.value = raidMeetup.meetupTime != DEFAULT_MEETUP_TIME
        numParticipants.value = raidMeetup.participantUserIds.size.toString()
        isUserParticipate.value = raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id)
        meetupTime.value = raidMeetup.meetupTime

        updateParticipantsList(raidMeetup)
    }

    fun reset() {
        Log.w(TAG, "Debug:: reset()")
        isRaidBossMissing.value = false
        raidImage.value = null
        isChangingMeetupTime.value = false

        isRaidMeetupAnnounced.value = false
        numParticipants.value = "0"
        participants.value = emptyList()
        isUserParticipate.value = false
        meetupTime.value = DEFAULT_MEETUP_TIME

        raidStateViewModel.reset()
    }

    fun changeParticipation(participate: Boolean) {

        raidMeetup?.id?.let {

            if (participate) {
                firebase.pushRaidMeetupParticipation(it)
            } else {
                firebase.cancelRaidMeetupParticipation(it)
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

        arena?.raid?.raidMeetupId?.let { raidMeetupId ->

            val meetupTime = TimeCalculator.format(hour, minutes)
            firebase.changeRaidMeetupTime(raidMeetupId, meetupTime)

        } ?: run {
            Log.e(TAG, "Could not push raid meetup, because raid is null of arena: $arena")
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