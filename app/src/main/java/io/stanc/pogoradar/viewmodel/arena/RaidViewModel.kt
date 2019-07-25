package io.stanc.pogoradar.viewmodel.arena

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.utils.Observables.dependantObservableField
import io.stanc.pogoradar.utils.TimeCalculator

class RaidViewModel: ViewModel() {
    private val TAG = javaClass.name

    private var arena: FirebaseArena? = null
    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private val raidStateViewModel = RaidStateViewModel()
    val raidMeetupViewModel = RaidMeetupViewModel()

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

    // RaidStateViewModel

    val raidState: MutableLiveData<RaidState> = raidStateViewModel.raidState
    val raidTime: MutableLiveData<String?> = raidStateViewModel.raidTime
    val isRaidAnnounced: MutableLiveData<Boolean> = raidStateViewModel.isRaidAnnounced

    // RaidMeetupViewModel

    val isRaidMeetupAnnounced: MutableLiveData<Boolean> = raidMeetupViewModel.isRaidMeetupAnnounced
    val meetupTime: MutableLiveData<String> = raidMeetupViewModel.meetupTime
    val numParticipants: MutableLiveData<String> = raidMeetupViewModel.numParticipants
    val participants: MutableLiveData<List<FirebasePublicUser>> = raidMeetupViewModel.participants
    val isUserParticipate: MutableLiveData<Boolean> = raidMeetupViewModel.isUserParticipate

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

    fun reset() {
        isRaidBossMissing.value = false
        raidStateViewModel.reset()
        raidMeetupViewModel.reset()
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
}