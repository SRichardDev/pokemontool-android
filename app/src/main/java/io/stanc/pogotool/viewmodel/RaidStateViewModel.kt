package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseRaid
import io.stanc.pogotool.utils.KotlinUtils
import io.stanc.pogotool.utils.TimeCalculator

class RaidStateViewModel(private var raid: FirebaseRaid?): ViewModel() {
    private val TAG = javaClass.name

    val raidState = ObservableField<RaidState>()
    // TODO: update raidTime ? maybe server should change: egg -> raid -> expired
    val raidTime = ObservableField<String?>()
    val isRaidAnnounced = ObservableField<Boolean>()

    init {
        updateData(raid)
    }

    fun updateData(raid: FirebaseRaid?) {
//        Log.d(TAG, "Debug:: updateData(raid: $raid)")
        this.raid = raid

        raid?.let {
            changeRaidData()
        } ?: kotlin.run {
            resetRaidData()
        }

//        Log.d(TAG, "Debug:: updateData(), isRaidAnnounced: ${isRaidAnnounced.get()}, raidState: ${raidState.get()?.name}, raidTime: ${raidTime.get()}")
    }

    private fun changeRaidData() {
        raidState.set(currentRaidState())
        raidTime.set(raidTime())
        isRaidAnnounced.set(currentRaidState() != RaidState.NONE)
    }

    private fun resetRaidData() {
        raidState.set(RaidState.NONE)
        raidTime.set(null)
        isRaidAnnounced.set(false)
    }

    enum class RaidState {
        NONE,
        EGG_HATCHES,
        RAID_RUNNING
    }

    // TODO: add timer for raidState and "raidIsExpired" + "eggIsHatching"
    fun currentRaidState(): RaidState {
//        Log.d(TAG, "Debug:: raidIsExpired: ${raidIsExpired()}, eggIsHatching: ${eggIsHatching()}")
        return when {
            raidIsExpired() -> RaidState.NONE
            eggIsHatching() -> RaidState.EGG_HATCHES
            else -> RaidState.RAID_RUNNING
        }
    }

    private fun eggIsHatching(): Boolean {

        return KotlinUtils.safeLet(raid?.timestamp as? Long, raid?.timeEggHatches) { timestamp, timeEggHatches ->

            TimeCalculator.timeExpired(timestamp, timeEggHatches)?.let { alreadyHatched ->
                !alreadyHatched
            } ?: kotlin.run {
                false
            }

        } ?: kotlin.run {
            false
        }
    }

    private fun raidIsExpired(): Boolean {

        return KotlinUtils.safeLet(raid?.timestamp as? Long, raid?.timeEnd) { timestamp, timeEnd ->

            TimeCalculator.timeExpired(timestamp, timeEnd)?.let {
                it
            } ?: kotlin.run {
                true
            }
        } ?: kotlin.run {
            true
        }
    }

    private fun raidTime(): String? {

        return when(currentRaidState()) {
            RaidState.EGG_HATCHES -> {
                timeEggHatches()
            }
            RaidState.RAID_RUNNING -> {
                timeRaidEnds()
            }
            else -> null
        }
    }

    fun timeEggHatches(): String? = raid?.timeEggHatches

    fun timeRaidEnds(): String? = raid?.timeEnd
}