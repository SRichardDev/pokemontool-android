package io.stanc.pogoradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import io.stanc.pogoradar.firebase.node.FirebaseRaid
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.TimeCalculator

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
        } ?: run {
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

    fun currentRaidState(): RaidState {
        return when {
            raidIsExpired() -> RaidState.NONE
            eggIsHatching() -> RaidState.EGG_HATCHES
            else -> RaidState.RAID_RUNNING
        }
    }

    private fun eggIsHatching(): Boolean {

        return Kotlin.safeLet(raid?.timestamp as? Long, raid?.timeEggHatches) { timestamp, timeEggHatches ->

            TimeCalculator.timeExpired(timestamp, timeEggHatches)?.let { alreadyHatched ->
                !alreadyHatched
            } ?: run {
                false
            }

        } ?: run {
            false
        }
    }

    private fun raidIsExpired(): Boolean {

        return Kotlin.safeLet(raid?.timestamp as? Long, raid?.timeEnd) { timestamp, timeEnd ->

            TimeCalculator.timeExpired(timestamp, timeEnd)?.let {
                it
            } ?: run {
                true
            }
        } ?: run {
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