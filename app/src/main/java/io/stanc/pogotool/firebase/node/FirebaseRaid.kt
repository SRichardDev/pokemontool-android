package io.stanc.pogotool.firebase.node

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.RAID
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_DURATION
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_LEVEL
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_MEETUP_ID
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_TIME_EGG_HATCHES
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_TIME_END
import io.stanc.pogotool.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogotool.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.TimeCalculator
import java.util.*

data class FirebaseRaid private constructor(override val id: String,
                                            val level: Int,
                                            val timeEggHatches: String?,
                                            val timeEnd: String,
                                            val timestamp: Any,
                                            val geoHash: GeoHash,
                                            val arenaId: String,
                                            var raidBossId: String? = null,
                                            var raidMeetupId: String? = null): FirebaseNode {

    override fun databasePath(): String = "$ARENAS/${firebaseGeoHash(geoHash)}/$arenaId/$RAID"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[RAID_LEVEL] = level
        timeEggHatches?.let {
            data[RAID_TIME_EGG_HATCHES] = it
        }
        data[RAID_TIME_END] = timeEnd
        data[TIMESTAMP] = timestamp
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
            val timeEggHatches = dataSnapshot.child(RAID_TIME_EGG_HATCHES).value as? String
            val timeEnd = dataSnapshot.child(RAID_TIME_END).value as? String
            val timestamp = dataSnapshot.child(TIMESTAMP).value as? Long

            val raidBossId = dataSnapshot.child(RAID_BOSS_ID).value as? String
            val raidMeetupId = dataSnapshot.child(RAID_MEETUP_ID).value as? String

//            Log.v(TAG, "id: $id, level: $level, timeEggHatches: $timeEggHatches, timeEnd: $timeEnd, timestamp: $timestamp, raidMeetupId: $raidMeetupId, raidBossId: $raidBossId")
            if (id != null && level != null && timeEnd != null && timestamp != null) {
                return FirebaseRaid(id, level.toInt(), timeEggHatches, timeEnd, timestamp, geoHash, arenaId, raidBossId, raidMeetupId)
            }

            return null
        }

        fun new(raidLevel: Int, timeEggHatchesHour: Int, timeEggHatchesMinutes: Int, geoHash: GeoHash, arenaId: String): FirebaseRaid {
//            Log.i(TAG, "Debug:: new Raid() timeEggHatchesHour: $timeEggHatchesHour, timeEggHatchesMinutes: $timeEggHatchesMinutes")
            val formattedTimeEggHatches = TimeCalculator.format(timeEggHatchesHour, timeEggHatchesMinutes)
            val formattedTimeRaidEnds = TimeCalculator.dateOfToday(formattedTimeEggHatches)?.let { dateEggHatches ->
                val date = TimeCalculator.addTime(dateEggHatches, RAID_DURATION)
                TimeCalculator.format(date)
            } ?: run { "00:00" }

//            Log.w(TAG, "Debug:: new Raid() formattedTimeEggHatches: $formattedTimeEggHatches, formattedTimeRaidEnds: $formattedTimeRaidEnds")
            return FirebaseRaid("", raidLevel, formattedTimeEggHatches, formattedTimeRaidEnds, FirebaseServer.timestamp(), geoHash, arenaId)
        }

        fun new(raidLevel: Int, timeRaidEndsHour: Int, timeRaidEndsMinutes: Int, geoHash: GeoHash, arenaId: String, raidBossId: String?): FirebaseRaid {
            val formattedTimeRaidEnds = TimeCalculator.format(timeRaidEndsHour, timeRaidEndsMinutes)

            return FirebaseRaid("", raidLevel, null, formattedTimeRaidEnds, FirebaseServer.timestamp(), geoHash, arenaId, raidBossId)
        }
    }
}