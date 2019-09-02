package io.stanc.pogoradar.viewmodel.arena

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.App
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
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
    private val raidMeetupViewModel = RaidMeetupViewModel()

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

    val isRaidBossMissing = MutableLiveData<Boolean>(false)
    val raidImage = MutableLiveData<Drawable?>()
    val isChangingMeetupTime = MutableLiveData<Boolean>(false)

    val isRaidMeetupAnnounced = MutableLiveData<Boolean>(false)
    val meetupTime = MutableLiveData<String>(App.geString(R.string.arena_raid_meetup_time_none))
    val numParticipants = MutableLiveData<String>("0")
    val participants = MutableLiveData<List<FirebasePublicUser>>(emptyList())
    val isUserParticipate = MutableLiveData<Boolean>(false)

    // RaidStateViewModel

    val raidState: MutableLiveData<RaidState> = raidStateViewModel.raidState
    val raidTime: MutableLiveData<String?> = raidStateViewModel.raidTime
    val isRaidAnnounced: MutableLiveData<Boolean> = raidStateViewModel.isRaidAnnounced

    override fun onCleared() {
        reset()
        super.onCleared()
    }

    /**
     * interface
     */

    fun updateData(arena: FirebaseArena?, context: Context) {
        this.arena = arena

        arena?.let {

            raidStateViewModel.updateData(arena.raid)
            raidImage.value = imageDrawable(arena, context)

            isRaidBossMissing.value = arena.raid?.let { raid ->
                raidState.value == RaidState.RAID_RUNNING && raid.raidBossId == null
            } ?: run {
                false
            }

            requestRaidMeetupData(arena)

        } ?: run {
            reset()
        }
    }

    // TODO: merge with other update method after RaidMeetup was moved to Arena in firebase database
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
        numParticipants.value = raidMeetup.participantUserIds.size.toString()
        isUserParticipate.value = raidMeetup.participantUserIds.contains(FirebaseUser.userData?.id)
        meetupTime.value = raidMeetup.meetupTime

        updateParticipantsList(raidMeetup)
    }

    fun reset() {
        isRaidBossMissing.value = false
        raidImage.value = null
        isChangingMeetupTime.value = false

        raidStateViewModel.reset()
        raidMeetupViewModel.reset()

        isRaidMeetupAnnounced.value = false
        numParticipants.value = "0"
        participants.value = emptyList()
        isUserParticipate.value = false
        meetupTime.value = App.geString(R.string.arena_raid_meetup_time_none)

        raidMeetupViewModel.raidMeetup?.let { firebase.removeObserver(raidMeetupObserver, it) }
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

    private fun requestRaidMeetupData(arena: FirebaseArena) {

        raidMeetupViewModel.raidMeetup?.let { firebase.removeObserver(raidMeetupObserver, it) }

        if (isRaidAnnounced.value == true) {
            arena.raid?.raidMeetupId?.let { id ->

                val raidMeetup = FirebaseRaidMeetup.new(id, DatabaseKeys.DEFAULT_MEETUP_TIME)
                firebase.addObserver(raidMeetupObserver, raidMeetup)

            } ?: run {
                raidMeetupViewModel.updateData(null)
            }
        }
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