package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_ARENA_RAID
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_ARENA_RAID_MEETUPS
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
                        val geoHash: GeoHash?,
                        val arenaId: String?,
                        var raidBossId: String? = null,
                        var raidMeetupId: String? = null): FirebaseNode {

    var meetup: FirebaseRaidMeetup? = null

    init {
//        TODO: should do fragment, because of lifecyle handling: removeNodeEventListener !!!
        raidMeetupId?.let { id ->
            FirebaseServer.addNodeEventListener("$DATABASE_ARENA_RAID_MEETUPS/$id", meetupDidChangeCallback)
        }
    }

    override fun databasePath(): String = "${FirebaseDatabase.DATABASE_ARENAS}/${geoHash.toString().substring(0, MapGridProvider.GEO_HASH_AREA_PRECISION)}/$arenaId/$DATABASE_ARENA_RAID"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["level"] = level
        data["timeLeftEggHatches"] = timeLeftEggHatches
        data["timeLeft"] = timeLeft
        data["timestamp"] = timestamp
        raidBossId?.let {
            data["raidBossId"] = it
        }
        raidMeetupId?.let {
            data["raidMeetupId"] = it
        }

        return data
    }

    /**
     * Node Listener
     */

    private val meetupDidChangeCallback = object: FirebaseServer.OnNodeDidChangeCallback {

        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            Log.i(TAG, "Debug:: nodeChanged(dataSnapshot: $dataSnapshot)")
            FirebaseRaidMeetup.new(dataSnapshot)?.let { meetup = it }
        }
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

        fun new(dataSnapshot: DataSnapshot): FirebaseRaid? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val level = dataSnapshot.child("level").value as? String
            val timeLeftEggHatches = dataSnapshot.child("timeLeftEggHatches").value as? String
            val timeLeft = dataSnapshot.child("timeLeft").value as? String
            val timestamp = dataSnapshot.child("timestamp").value as? Long

            val raidBossId = dataSnapshot.child("raidBossId").value as? String
            val raidMeetupId = dataSnapshot.child("raidMeetupId").value as? String

//            Log.v(TAG, "id: $id, level: $level, timeLeftEggHatches: $timeLeftEggHatches, timeLeft: $timeLeft, timestamp: $timestamp, raidMeetupId: $raidMeetupId, raidBossId: $raidBossId")
            if (id != null && level != null && timeLeftEggHatches != null && timeLeft != null && timestamp != null) {
                return FirebaseRaid(id, level, timeLeftEggHatches, timeLeft, timestamp, null, null, raidBossId, raidMeetupId)
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