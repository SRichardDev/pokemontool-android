package io.stanc.pogoradar.viewmodel.arena

import android.os.Parcelable
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.node.FirebaseRaid
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

    return raid?.eggHatchesTimestamp?.let { timestampEggHatches ->
        !TimeCalculator.timeExpired(timestampEggHatches)
    } ?: run {
        false
    }
}

private fun raidIsExpired(raid: FirebaseRaid?): Boolean {

    return raid?.endTimestamp?.let { timestampEnd ->
        TimeCalculator.timeExpired(timestampEnd)
    } ?: run {
        true
    }
}

fun raidTime(raid: FirebaseRaid?): String? {

    if (raid == null) {
        return null
    }

    return when(currentRaidState(raid)) {
        RaidState.EGG_HATCHES -> {
            if(raid.eggHatchesTimestamp != DatabaseKeys.TIMESTAMP_NONE) TimeCalculator.format(raid.eggHatchesTimestamp) else DatabaseKeys.DEFAULT_TIME
        }
        RaidState.RAID_RUNNING -> {
            if(raid.endTimestamp != DatabaseKeys.TIMESTAMP_NONE) TimeCalculator.format(raid.endTimestamp) else DatabaseKeys.DEFAULT_TIME
        }
        else -> null
    }
}