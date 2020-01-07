package io.stanc.pogoradar.firebase.node

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.EMAIL
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOKEN
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOPIC_ANDROID
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_QUESTS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_RAIDS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_GEOHASHES
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_APP_LAST_OPENED
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_CODE
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_NAME
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_PLATFORM
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_TEAM
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_TOPICS
import io.stanc.pogoradar.geohash.GeoHash

enum class Team {
    MYSTIC,
    VALOR,
    INSTINCT;

    fun toNumber(): Number = ordinal

    companion object {
        fun valueOf(number: Number): Team? = Team.values().find { it.ordinal == number.toInt() }
    }
}

data class FirebaseUserNode private constructor(override var id: String,
                                                var email: String,
                                                var name: String,
                                                var team: Team,
                                                var level: Number,
                                                var notificationToken: String? = null,
                                                var platform: String? = null,
                                                var code: String? = null,
                                                var submittedArenas: Number = 0,
                                                var submittedPokestops: Number = 0,
                                                var submittedQuests: Number = 0,
                                                var submittedRaids: Number = 0,
                                                var subscribedGeohashes: List<GeoHash>? = emptyList(),
                                                var subscribedRaidMeetups: List<String>? = emptyList(),
                                                var notificationTopics: List<String>? = emptyList(),
                                                var isNotificationActive: Boolean = true,
                                                var timestampAppLastOpened: Number? = null,
                                                var photoURL: Uri? = null): FirebaseDataNode {

    override fun databasePath(): String {
        return USERS
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[USER_ID] = id
        data[EMAIL] = email
        notificationToken?.let { data[NOTIFICATION_TOKEN] = it }
        platform?.let { data[USER_PLATFORM] = it }
        timestampAppLastOpened?.let { data[USER_APP_LAST_OPENED] = it }


        val publicData = HashMap<String, Any>()
        publicData[USER_NAME] = name
        publicData[USER_TEAM] = team.toNumber()
        publicData[USER_LEVEL] = level
        code?.let { publicData[USER_CODE] = it }

        data[USER_PUBLIC_DATA] = publicData

        // TODO: submitted stuff ???

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseUserNode? {

//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val email = dataSnapshot.child(EMAIL).value as? String
            val notificationToken = dataSnapshot.child(NOTIFICATION_TOKEN).value as? String
            val platform = dataSnapshot.child(USER_PLATFORM).value as? String
            val timestampAppLastOpened = dataSnapshot.child(USER_APP_LAST_OPENED).value as? Number

            val submittedArenas =
                (dataSnapshot.child(SUBMITTED_ARENAS) as? DataSnapshot)?.childrenCount ?: run { null }
            val submittedPokestops =
                (dataSnapshot.child(SUBMITTED_POKESTOPS) as? DataSnapshot)?.childrenCount ?: run { null }
            val submittedQuests = dataSnapshot.child(SUBMITTED_QUESTS).value as? Number
            val submittedRaids = dataSnapshot.child(SUBMITTED_RAIDS).value as? Number

            // public data
            val name = dataSnapshot.child(USER_PUBLIC_DATA).child(USER_NAME).value as? String
            val team = (dataSnapshot.child(USER_PUBLIC_DATA).child(USER_TEAM).value as? Number)?.let { teamNumber ->
                Team.valueOf(teamNumber)
            }
            val code = dataSnapshot.child(USER_PUBLIC_DATA).child(USER_CODE).value as? String
            val level = dataSnapshot.child(USER_PUBLIC_DATA).child(USER_LEVEL).value as? Number

            val subscribedGeohashes = (dataSnapshot.child(SUBSCRIBED_GEOHASHES) as? DataSnapshot)?.children?.mapNotNull { child -> child.key?.let { GeoHash(it) } }?.toList()
            val subscribedRaidMeetups = (dataSnapshot.child(SUBSCRIBED_RAID_MEETUPS) as? DataSnapshot)?.children?.mapNotNull { child -> child.key }?.toList()
            val topics = (dataSnapshot.child(USER_TOPICS) as? DataSnapshot)?.children?.mapNotNull { child -> child.key }?.toList()

//            Log.v(TAG, "id: $id, name: $name, email: $email, team: $team, level: $level, notificationToken: $notificationToken, submittedArenas: $submittedArenas, submittedPokestops: $submittedPokestops")

            if (id != null && name != null && email != null && team != null && level != null) {
                val user = new(id, email, name, team, level)

                // optionals
                code?.let { user.code = it }

                notificationToken?.let { user.notificationToken = it }
                platform?.let { user.platform = it }
                timestampAppLastOpened?.let { user.timestampAppLastOpened = it }

                submittedArenas?.let { user.submittedArenas = it }
                submittedPokestops?.let { user.submittedPokestops = it }
                submittedQuests?.let { user.submittedQuests = it }
                submittedRaids?.let { user.submittedRaids = it }

                subscribedGeohashes?.let { user.subscribedGeohashes = it }
                subscribedRaidMeetups?.let { user.subscribedRaidMeetups = it }
                topics?.let {
                    user.notificationTopics = it
                    user.isNotificationActive = it.contains(NOTIFICATION_TOPIC_ANDROID)
                } ?: run {
                    user.isNotificationActive = false
                }

                return user
            }

            return null
        }

        fun new(uid: String, email: String, name: String, team: Team, level: Number): FirebaseUserNode {
            return FirebaseUserNode(uid, email, name, team, level)
        }
    }
}