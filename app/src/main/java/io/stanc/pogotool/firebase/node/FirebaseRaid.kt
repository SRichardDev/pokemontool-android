package io.stanc.pogotool.firebase.node

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.RAID
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_LEVEL
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_MEETUP_ID
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_TIME_LEFT
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_TIME_LEFT_EGG_HATCHES
import io.stanc.pogotool.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.MapGridProvider
import io.stanc.pogotool.utils.TimeCalculator
import java.util.*

data class FirebaseRaid(override val id: String,
                        val level: String,
                        val timeLeftEggHatches: String,
                        val timeLeft: String,
                        val timestamp: Any,
                        val geoHash: GeoHash,
                        val arenaId: String,
                        var raidBossId: String? = null,
                        var raidMeetupId: String? = null): FirebaseNode {

    override fun databasePath(): String = "$ARENAS/${geoHash.toString().substring(0, MapGridProvider.GEO_HASH_AREA_PRECISION)}/$arenaId/$RAID"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[RAID_LEVEL] = level
        data[RAID_TIME_LEFT_EGG_HATCHES] = timeLeftEggHatches
        data[RAID_TIME_LEFT] = timeLeft
        data[TIMESTAMP] = timestamp
        raidBossId?.let {
            data[RAID_BOSS_ID] = it
        }
        raidMeetupId?.let {
            data[RAID_MEETUP_ID] = it
        }

        return data
    }

    /**
     * Raid states
     */

    enum class RaidState {
        NONE,
        EGG_HATCHES,
        RAID_RUNNING
    }

    fun currentRaidState(): RaidState {

        return if (raidIsExpired()) {
            RaidState.NONE
        } else {
            eggIsAlreadyHatched()?.let { alreadyHatched ->

                if (alreadyHatched) {
                    RaidState.RAID_RUNNING
                } else {
                    RaidState.EGG_HATCHES
                }

            } ?: kotlin.run {
                RaidState.NONE
            }
        }
    }

    private fun eggIsAlreadyHatched(): Boolean? {
        return dateEggHatches()?.let {
            return TimeCalculator.timeExpired(it) || timeLeftEggHatches.toInt() == 0
        } ?: kotlin.run {
            null
        }
    }

    private fun raidIsExpired(): Boolean {
        return dateRaidEnds()?.let {
            return TimeCalculator.timeExpired(it)
        } ?: kotlin.run {
            true
        }
    }

    /**
     * Egg time
     */

    fun timeEggHatches(): String? = dateEggHatches()?.let { TimeCalculator.format(it) } ?: kotlin.run { null }

    private fun dateEggHatches(): Date? {
        return (timestamp as? Long)?.let {
           TimeCalculator.addTime(it, timeLeftEggHatches)
        } ?: kotlin.run {
            null
        }
    }

    /**
     * Raid time
     */

    fun timeRaidEnds(): String? = dateRaidEnds()?.let { TimeCalculator.format(it) } ?: kotlin.run { null }

    private fun dateRaidEnds(): Date? {
        return (timestamp as? Long)?.let {
            TimeCalculator.addTime(it, timeLeft)
        } ?: kotlin.run {
            null
        }
    }


    companion object {

        private val TAG = javaClass.name

        fun new(arenaId: String, geoHash: GeoHash, dataSnapshot: DataSnapshot): FirebaseRaid? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val level = dataSnapshot.child(RAID_LEVEL).value as? String
            val timeLeftEggHatches = dataSnapshot.child(RAID_TIME_LEFT_EGG_HATCHES).value as? String
            val timeLeft = dataSnapshot.child(RAID_TIME_LEFT).value as? String
            val timestamp = dataSnapshot.child(TIMESTAMP).value as? Long

            val raidBossId = dataSnapshot.child(RAID_BOSS_ID).value as? String
            val raidMeetupId = dataSnapshot.child(RAID_MEETUP_ID).value as? String

//            Log.v(TAG, "id: $id, level: $level, timeLeftEggHatches: $timeLeftEggHatches, timeLeft: $timeLeft, timestamp: $timestamp, raidMeetupId: $raidMeetupId, raidBossId: $raidBossId")
            if (id != null && level != null && timeLeftEggHatches != null && timeLeft != null && timestamp != null) {
                return FirebaseRaid(id, level, timeLeftEggHatches, timeLeft, timestamp, geoHash, arenaId, raidBossId, raidMeetupId)
            }

            return null
        }

        fun new(raidLevel: Int, timeLeftEggHatchesInMinutes: Int, geoHash: GeoHash, arenaId: String): FirebaseRaid {
            return FirebaseRaid("", raidLevel.toString(), timeLeftEggHatchesInMinutes.toString(), "45", FirebaseServer.timestamp(), geoHash, arenaId)
        }

        fun new(raidLevel: Int, timeLeftRaidRunningInMinutes: Int, geoHash: GeoHash, arenaId: String, raidBossId: String?): FirebaseRaid {
            return FirebaseRaid("", raidLevel.toString(), "0", timeLeftRaidRunningInMinutes.toString(), FirebaseServer.timestamp(), geoHash, arenaId, raidBossId)
        }
    }
}