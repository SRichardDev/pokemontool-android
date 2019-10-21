package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUP
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_TIME_EGG_HATCHES
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_TIME_END
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.firebase.FirebaseServer.TIMESTAMP_SERVER
import io.stanc.pogoradar.viewmodel.arena.RaidState
import io.stanc.pogoradar.viewmodel.arena.currentRaidState
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class FirebaseRaid private constructor(override val id: String,
                                            val parentDatabasePath: String,
                                            val level: Int,
                                            val timeEggHatches: String?,
                                            val timeEnd: String,
                                            val timestamp: Long,
                                            var raidBossId: String? = null,
                                            var raidMeetup: FirebaseRaidMeetup? = null,
                                            var latestRaidState: RaidState = RaidState.NONE): FirebaseNode, Parcelable {

    init {
        latestRaidState = currentRaidState(this)
    }

    override fun databasePath(): String = parentDatabasePath

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[RAID_LEVEL] = level
        timeEggHatches?.let {
            data[RAID_TIME_EGG_HATCHES] = it
        }
        data[RAID_TIME_END] = timeEnd
        data[TIMESTAMP] = if(timestamp == TIMESTAMP_SERVER) FirebaseServer.timestamp() else timestamp
        raidBossId?.let {
            data[RAID_BOSS_ID] = it
        }
        raidMeetup?.let {
            data[RAID_MEETUP] = it.data()
        }

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(parentDatabasePath: String, dataSnapshot: DataSnapshot): FirebaseRaid? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val level = dataSnapshot.child(RAID_LEVEL).value as? Number
            val timeEggHatches = dataSnapshot.child(RAID_TIME_EGG_HATCHES).value as? String
            val timeEnd = dataSnapshot.child(RAID_TIME_END).value as? String
            val timestamp = dataSnapshot.child(TIMESTAMP).value as? Long
            val raidBossId = dataSnapshot.child(RAID_BOSS_ID).value as? String

//            Log.v(TAG, "id: $id, level: $level, timeEggHatches: $timeEggHatches, timeEnd: $timeEnd, timestamp: $timestamp, raidMeetupId: $raidMeetupId, raidBossId: $raidBossId")
            if (id != null && level != null && timeEnd != null && timestamp != null) {

                val databasePath = "$parentDatabasePath/$RAID_MEETUP"
                val raidMeetup = FirebaseRaidMeetup.new(databasePath, dataSnapshot.child(RAID_MEETUP))

                return FirebaseRaid(id, parentDatabasePath, level.toInt(), timeEggHatches, timeEnd, timestamp, raidBossId, raidMeetup)
            }

            return null
        }

        /**
         * Egg
         */

        // TODO: new(...) { id: String = DatabaseKeys.Raid }

//        fun new(raidLevel: Int, timeEggHatchesHour: Int, timeEggHatchesMinutes: Int, geoHash: GeoHash, arenaId: String): FirebaseRaid {
////            Log.i(TAG, "Debug:: new Raid() timeEggHatchesHour: $timeEggHatchesHour, timeEggHatchesMinutes: $timeEggHatchesMinutes")
//            val formattedTimeEggHatches = TimeCalculator.format(timeEggHatchesHour, timeEggHatchesMinutes)
//            val formattedTimeRaidEnds = TimeCalculator.dateOfToday(formattedTimeEggHatches)?.let { dateEggHatches ->
//                val date = TimeCalculator.addTime(dateEggHatches, RAID_DURATION)
//                TimeCalculator.format(date)
//            } ?: run {
//                DATA_UNDEFINED
//            }
//
////            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
//            return FirebaseRaid("", raidLevel, formattedTimeEggHatches, formattedTimeRaidEnds, TIMESTAMP_SERVER, geoHash, arenaId)
//        }
//
//        fun new(raidLevel: Int, timeEggHatchesMinutes: Int, geoHash: GeoHash, arenaId: String): FirebaseRaid {
//            val timeEggHatchesDate = TimeCalculator.addTime(TimeCalculator.currentDate(), timeEggHatchesMinutes)
//            val formattedTimeEggHatches = TimeCalculator.format(timeEggHatchesDate)
////            Log.i(TAG, "Debug:: new Raid($geoHash, $arenaId) timeEggHatchesMinutes: $timeEggHatchesMinutes, timeEggHatchesDate: $timeEggHatchesDate, formattedTimeEggHatches: $formattedTimeEggHatches")
//            val formattedTimeRaidEnds = TimeCalculator.dateOfToday(formattedTimeEggHatches)?.let { dateEggHatches ->
//                val date = TimeCalculator.addTime(dateEggHatches, RAID_DURATION)
//                TimeCalculator.format(date)
//            } ?: run {
//                DATA_UNDEFINED
//            }
//
////            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
//            return FirebaseRaid("", raidLevel, formattedTimeEggHatches, formattedTimeRaidEnds, TIMESTAMP_SERVER, geoHash, arenaId)
//        }
//
//        /**
//         * Raid
//         */
//
//        fun new(raidLevel: Int, timeRaidEndsHour: Int, timeRaidEndsMinutes: Int, geoHash: GeoHash, arenaId: String, raidBossId: String?): FirebaseRaid {
//
//            val formattedTimeRaidEnds = TimeCalculator.format(timeRaidEndsHour, timeRaidEndsMinutes)
//            val formattedTimeEggHatches = TimeCalculator.dateOfToday(formattedTimeRaidEnds)?.let {
//                TimeCalculator.format(TimeCalculator.addTime(it, -RAID_DURATION))
//            } ?: run {
//                DATA_UNDEFINED
//            }
////            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
//
//            return FirebaseRaid("", raidLevel, formattedTimeEggHatches, formattedTimeRaidEnds, TIMESTAMP_SERVER, geoHash, arenaId, raidBossId)
//        }
//
//        fun new(raidLevel: Int, timeRaidEndsMinutes: Int, geoHash: GeoHash, arenaId: String, raidBossId: String?): FirebaseRaid {
//
//            val timeRaidEndsDate = TimeCalculator.addTime(TimeCalculator.currentDate(), timeRaidEndsMinutes)
//            val formattedTimeRaidEnds = TimeCalculator.format(timeRaidEndsDate)
////            Log.i(TAG, "Debug:: new Raid($geoHash, $arenaId) timeRaidEndsMinutes: $timeRaidEndsMinutes, timeRaidEndsDate: $timeRaidEndsDate, formattedTimeRaidEnds: $formattedTimeRaidEnds")
//
//            val formattedTimeEggHatches = TimeCalculator.dateOfToday(formattedTimeRaidEnds)?.let {
//                TimeCalculator.format(TimeCalculator.addTime(it, -RAID_DURATION))
//            } ?: run {
//                DATA_UNDEFINED
//            }
////            Log.d(TAG, "Debug:: new raid: formattedTimeRaidEnds: $formattedTimeRaidEnds, formattedTimeEggHatches: $formattedTimeEggHatches")
//
//            return FirebaseRaid("", raidLevel, formattedTimeEggHatches, formattedTimeRaidEnds, TIMESTAMP_SERVER, geoHash, arenaId, raidBossId)
//        }
    }
}