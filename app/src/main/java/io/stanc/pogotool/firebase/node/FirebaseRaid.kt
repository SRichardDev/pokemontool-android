package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_RAID
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.MapGridProvider
import io.stanc.pogotool.utils.TimeCalculator
import java.text.DateFormat
import java.text.SimpleDateFormat
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

    init {
        eggIsAlreadyHatched()
    }

    override fun databasePath(): String = "${FirebaseDatabase.DATABASE_ARENAS}/${geoHash.toString().substring(0, MapGridProvider.GEO_HASH_AREA_PRECISION)}/$arenaId/$DATABASE_RAID"

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

    fun eggIsAlreadyHatched(): Boolean? {
        (timestamp as? Long)?.let {
            return TimeCalculator.timeExpired(it, timeLeftEggHatches)
        }

        return null
    }

    fun timeEggHatches(): String? {
        (timestamp as? Long)?.let {
            val date = TimeCalculator.addTime(it, timeLeftEggHatches)
            return TimeCalculator.format(date)
        }

        return null
    }

    fun timeRaidStarts(): String? {
        (timestamp as? Long)?.let {
            val date = TimeCalculator.addTime(it, timeLeft)
            return TimeCalculator.format(date)
        }

        return null
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaid? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

// TODO:            arenaId und geohash auslesen? oder als paramter in .new(...) mit geben

            val id = dataSnapshot.key
            val level = dataSnapshot.child("level").value as? String
            val timeLeftEggHatches = dataSnapshot.child("timeLeftEggHatches").value as? String
            val timeLeft = dataSnapshot.child("timeLeft").value as? String
            val timestamp = dataSnapshot.child("timestamp").value as? Long

            val raidBossId = dataSnapshot.child("raidBossId").value as? String
            val raidMeetupId = dataSnapshot.child("raidMeetupId").value as? String


            Log.v(TAG, "id: $id, level: $level, timeLeftEggHatches: $timeLeftEggHatches, timeLeft: $timeLeft, timestamp: $timestamp, raidMeetupId: $raidMeetupId, raidBossId: $raidBossId")

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