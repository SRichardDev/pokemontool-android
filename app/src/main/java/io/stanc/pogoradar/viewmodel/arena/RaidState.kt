package io.stanc.pogoradar.viewmodel.arena

import android.os.Parcelable
import io.stanc.pogoradar.firebase.node.FirebaseRaid
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.TimeCalculator
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class RaidState: Parcelable {
    NONE,
    EGG_HATCHES,
    RAID_RUNNING
}

fun currentRaidState(raid: FirebaseRaid?): RaidState {
    return when {
        raidIsExpired(raid) -> RaidState.NONE
        eggIsHatching(raid) -> RaidState.EGG_HATCHES
        else -> RaidState.RAID_RUNNING
    }
}

private fun eggIsHatching(raid: FirebaseRaid?): Boolean {

    return Kotlin.safeLet(raid?.timestamp, raid?.timeEggHatches) { timestamp, timeEggHatches ->

        TimeCalculator.timeExpired(timestamp, timeEggHatches)?.let { alreadyHatched ->
            !alreadyHatched
        } ?: run {
            false
        }

    } ?: run {
        false
    }
}

private fun raidIsExpired(raid: FirebaseRaid?): Boolean {

    return Kotlin.safeLet(raid?.timestamp, raid?.timeEnd) { timestamp, timeEnd ->

        TimeCalculator.timeExpired(timestamp, timeEnd)?.let {
            it
        } ?: run {
            true
        }
    } ?: run {
        true
    }
}

fun raidTime(raid: FirebaseRaid?): String? {

    return when(currentRaidState(raid)) {
        RaidState.EGG_HATCHES -> {
            raid?.timeEggHatches
        }
        RaidState.RAID_RUNNING -> {
            raid?.timeEnd
        }
        else -> null
    }
}