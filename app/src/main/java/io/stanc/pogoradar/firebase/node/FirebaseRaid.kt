package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSS_DEFAULT
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_DURATION
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUP
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_TIME_EGG_HATCHES
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_TIME_END
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTER_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP_NONE
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.TimeCalculator
import io.stanc.pogoradar.viewmodel.arena.RaidState
import io.stanc.pogoradar.viewmodel.arena.currentRaidState
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class FirebaseRaid private constructor(override val id: String,
                                            val parentDatabasePath: String,
                                            val level: Int,
                                            val eggHatchesTimestamp: Long,
                                            val endTimestamp: Long,
                                            val raidId: String,
                                            var raidBoss: Number,
                                            var submitterId: String,
                                            var timestamp: Long,
                                            var raidMeetup: FirebaseRaidMeetup? = null,
                                            var latestRaidState: RaidState = RaidState.NONE): FirebaseNode, Parcelable {

    init {
        latestRaidState = currentRaidState(this)
    }

    override fun databasePath(): String = parentDatabasePath

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[RAID_LEVEL] = level
        data[RAID_TIME_EGG_HATCHES] = eggHatchesTimestamp
        data[RAID_TIME_END] = endTimestamp
        data[RAID_TIME_END] = endTimestamp
        data[RAID_ID] = raidId
        data[RAID_BOSS] = raidBoss
        data[SUBMITTER_ID] = submitterId
        data[TIMESTAMP] = timestamp

        raidMeetup?.let {
            data[RAID_MEETUP] = it.data()
        }

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun parentDatabasePath(geoHash: GeoHash, arenaId: String): String {
            return "${DatabaseKeys.ARENAS}/${DatabaseKeys.firebaseGeoHash(geoHash)}/$arenaId"
        }

        fun new(parentDatabasePath: String, dataSnapshot: DataSnapshot): FirebaseRaid? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key

            val level = dataSnapshot.child(RAID_LEVEL).value as? Number
            val timestampEggHatches = dataSnapshot.child(RAID_TIME_EGG_HATCHES).value as? Long
            val timestampEnd = dataSnapshot.child(RAID_TIME_END).value as? Long
            val raidBoss = dataSnapshot.child(RAID_BOSS).value as? Number
            val raidId = dataSnapshot.child(RAID_ID).value as? String
            val submitterId = dataSnapshot.child(SUBMITTER_ID).value as? String
            val timestamp =  dataSnapshot.child(TIMESTAMP).value as? Long

            Log.v(TAG, "id: $id, level: $level, timestampEggHatches: $timestampEggHatches, timestampEnd: $timestampEnd, raidBoss: $raidBoss, raidId: $raidId, submitterId: $submitterId, timestamp: $timestamp")
            if (id != null && level != null && timestampEggHatches != null && timestampEnd != null && raidBoss != null && raidId != null && submitterId != null && timestamp != null) {

                val raidMeetup = if (dataSnapshot.hasChild(RAID_MEETUP)) {
                    val raidMeetupNode = dataSnapshot.child(RAID_MEETUP)
                    val raidMeetupParentDatabasePath = "$parentDatabasePath/$id"
                    FirebaseRaidMeetup.new(raidMeetupParentDatabasePath, raidMeetupNode)
                } else {
                    null
                }

                return FirebaseRaid(id, parentDatabasePath, level.toInt(), timestampEggHatches, timestampEnd, raidId, raidBoss, submitterId, timestamp, raidMeetup)
            }

            return null
        }

        /**
         * Egg
         */

        fun newEgg(parentDatabasePath: String, raidLevel: Int, raidId: String, timeEggHatchesHour: Int, timeEggHatchesMinutes: Int): FirebaseRaid? {

            val id = RAID
            val timestampEggHatches = TimeCalculator.timestampOfToday(timeEggHatchesHour, timeEggHatchesMinutes) ?: TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestamp(timestampEggHatches, RAID_DURATION) ?: TIMESTAMP_NONE

            val submitterId = FirebaseUser.userData?.id ?: run {
                Log.e(TAG, "could not create raid, because user is not logged in properly!")
                return null
            }

            val timestamp = TimeCalculator.currentTimestamp()

            return FirebaseRaid(id, parentDatabasePath, raidLevel, timestampEggHatches, timestampEnd, raidId, RAID_BOSS_DEFAULT, submitterId, timestamp)
        }

        fun newEgg(parentDatabasePath: String, raidLevel: Int, raidId: String, timeEggHatchesMinutes: Int): FirebaseRaid? {

            val id = RAID
            val timestampEggHatches = TimeCalculator.timestamp(TimeCalculator.currentDate(), timeEggHatchesMinutes) ?: TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestamp(timestampEggHatches, RAID_DURATION) ?: TIMESTAMP_NONE

            val submitterId = FirebaseUser.userData?.id ?: run {
                Log.e(TAG, "could not create raid, because user is not logged in properly!")
                return null
            }

            val timestamp = TimeCalculator.currentTimestamp()

            return FirebaseRaid(id, parentDatabasePath, raidLevel, timestampEggHatches, timestampEnd, raidId, RAID_BOSS_DEFAULT, submitterId, timestamp)
        }

        /**
         * Raid
         */

        fun newRaid(parentDatabasePath: String, raidLevel: Int, raidId: String, raidBoss: Int, timeRaidEndsHour: Int, timeRaidEndsMinutes: Int): FirebaseRaid? {

            val id = RAID
            val timestampEggHatches = TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestampOfToday(timeRaidEndsHour, timeRaidEndsMinutes) ?: TIMESTAMP_NONE

            val submitterId = FirebaseUser.userData?.id ?: run {
                Log.e(TAG, "could not create raid, because user is not logged in properly!")
                return null
            }

            val timestamp = TimeCalculator.currentTimestamp()

            return FirebaseRaid(id, parentDatabasePath, raidLevel, timestampEggHatches, timestampEnd, raidId, raidBoss, submitterId, timestamp)
        }

        fun newRaid(parentDatabasePath: String, raidLevel: Int, raidId: String, raidBoss: Int, timeRaidEndsMinutes: Int): FirebaseRaid? {

            val id = RAID
            val timestampEggHatches = TIMESTAMP_NONE
            val timestampEnd = TimeCalculator.timestamp(TimeCalculator.currentDate(), timeRaidEndsMinutes) ?: TIMESTAMP_NONE

            val submitterId = FirebaseUser.userData?.id ?: run {
                Log.e(TAG, "could not create raid, because user is not logged in properly!")
                return null
            }

            val timestamp = TimeCalculator.currentTimestamp()

            return FirebaseRaid(id, parentDatabasePath, raidLevel, timestampEggHatches, timestampEnd, raidId, raidBoss, submitterId, timestamp)
        }
    }
}