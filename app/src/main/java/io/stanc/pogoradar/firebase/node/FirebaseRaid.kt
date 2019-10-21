package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_DURATION
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUP_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_TIME_EGG_HATCHES
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_TIME_END
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP_NONE
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.TimeCalculator
import io.stanc.pogoradar.viewmodel.arena.RaidState
import io.stanc.pogoradar.viewmodel.arena.currentRaidState
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class FirebaseRaid private constructor(override val id: String,
                                            val level: Int,
                                            val timestampEggHatches: Long,
                                            val timestampEnd: Long,
                                            val geoHash: GeoHash,
                                            val arenaId: String,
                                            var raidBossId: String? = null,
                                            var raidMeetupId: String? = null,
                                            var latestRaidState: RaidState = RaidState.NONE): FirebaseNode, Parcelable {

    init {
        latestRaidState = currentRaidState(this)
    }

    override fun databasePath(): String = "$ARENAS/${firebaseGeoHash(geoHash)}/$arenaId/$RAID"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[RAID_LEVEL] = level
        data[RAID_TIME_EGG_HATCHES] = timestampEggHatches
        data[RAID_TIME_END] = timestampEnd
        raidBossId?.let {
            data[RAID_BOSS_ID] = it
        }
        raidMeetupId?.let {
            data[RAID_MEETUP_ID] = it
        }

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(arenaId: String, geoHash: GeoHash, dataSnapshot: DataSnapshot): FirebaseRaid? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val level = dataSnapshot.child(RAID_LEVEL).value as? Number
            val timestampEggHatches = dataSnapshot.child(RAID_TIME_EGG_HATCHES).value as? Long
            val timestampEnd = dataSnapshot.child(RAID_TIME_END).value as? Long

            val raidBossId = dataSnapshot.child(RAID_BOSS_ID).value as? String
            val raidMeetupId = dataSnapshot.child(RAID_MEETUP_ID).value as? String

//            Log.v(TAG, "id: $id, level: $level, timeEggHatches: $timeEggHatches, timeEnd: $timeEnd, timestamp: $timestamp, raidMeetupId: $raidMeetupId, raidBossId: $raidBossId")
            if (id != null && level != null && timestampEggHatches != null && timestampEnd != null) {
                return FirebaseRaid(id, level.toInt(), timestampEggHatches, timestampEnd, geoHash, arenaId, raidBossId, raidMeetupId)
            }

            return null
        }

        /**
         * Egg
         */

        fun new(raidLevel: Int, timeEggHatchesHour: Int, timeEggHatchesMinutes: Int, geoHash: GeoHash, arenaId: String): FirebaseRaid {

            val timestampEggHatches = TimeCalculator.timestampOfToday(timeEggHatchesHour, timeEggHatchesMinutes) ?: TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestamp(timestampEggHatches, RAID_DURATION) ?: TIMESTAMP_NONE

//            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
            return FirebaseRaid("", raidLevel, timestampEggHatches, timestampEnd, geoHash, arenaId)
        }

        fun new(raidLevel: Int, timeEggHatchesMinutes: Int, geoHash: GeoHash, arenaId: String): FirebaseRaid {

            val timestampEggHatches = TimeCalculator.timestamp(TimeCalculator.currentDate(), timeEggHatchesMinutes) ?: TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestamp(timestampEggHatches, RAID_DURATION) ?: TIMESTAMP_NONE

//            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
            return FirebaseRaid("", raidLevel, timestampEggHatches, timestampEnd, geoHash, arenaId)
        }

        /**
         * Raid
         */

        fun new(raidLevel: Int, timeRaidEndsHour: Int, timeRaidEndsMinutes: Int, geoHash: GeoHash, arenaId: String, raidBossId: String?): FirebaseRaid {

            val timestampEggHatches = TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestampOfToday(timeRaidEndsHour, timeRaidEndsMinutes) ?: TIMESTAMP_NONE

//            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
            return FirebaseRaid("", raidLevel, timestampEggHatches, timestampEnd, geoHash, arenaId, raidBossId)
        }

        fun new(raidLevel: Int, timeRaidEndsMinutes: Int, geoHash: GeoHash, arenaId: String, raidBossId: String?): FirebaseRaid {

            val timestampEggHatches = TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestamp(TimeCalculator.currentDate(), timeRaidEndsMinutes) ?: TIMESTAMP_NONE

//            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
            return FirebaseRaid("", raidLevel, timestampEggHatches, timestampEnd, geoHash, arenaId, raidBossId)
        }
    }
}