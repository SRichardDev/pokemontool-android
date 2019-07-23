package io.stanc.pogoradar.viewmodel.arena

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import androidx.lifecycle.MutableLiveData
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
    val raidImage = ObservableField<Drawable?>()

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

    val isRaidBossMissing = MutableLiveData<Boolean>() // isRaidAnnounced && raidState == RaidState.RAID_RUNNING && arena.raid?.raidBossId == null

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
        raidMeetupViewModel.raidMeetup?.let { firebase.removeObserver(raidMeetupObserver, it) }
        super.onCleared()
    }

    /**
     * interface
     */

    fun updateData(arena: FirebaseArena?, context: Context) {
        this.arena = arena

        arena?.let {

            requestRaidMeetupData(arena)

            raidStateViewModel.updateData(arena.raid)
            raidImage.set(imageDrawable(arena, context))

            arena.raid?.let { raid ->

                isRaidBossMissing.value = isRaidAnnounced.value == true && raid.raidBossId == null

            } ?: run {

                isRaidBossMissing.value = false
            }

            Log.i(TAG, "Debug:: updateData($arena) isRaidBossMissing: ${isRaidBossMissing.value}")

        } ?: run {
            reset()
        }

//        Log.i(TAG, "Debug:: updateData(), isRaidBossMissing: ${isRaidBossMissing.get()}, isRaidAnnounced: ${isRaidAnnounced.get()}, raidState: ${raidState.get()?.name}, raidTime: ${raidTime.get()}")
    }

    fun reset() {
        isRaidBossMissing.value = false
        raidStateViewModel.reset()
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

    fun changeMeetupTime(hour: Int, minutes: Int) {

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

        arena.raid?.raidMeetupId?.let { id ->

            val raidMeetup = FirebaseRaidMeetup.new(id, DatabaseKeys.DEFAULT_MEETUP_TIME)
            firebase.addObserver(raidMeetupObserver, raidMeetup)

        } ?: run {
            raidMeetupViewModel.updateData(null)
        }
    }
}