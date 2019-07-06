package io.stanc.pogoradar.firebase.node

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.EMAIL
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_ACTIVE
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOKEN
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_QUESTS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_RAIDS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_GEOHASH_ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_GEOHASH_POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_CODE
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_NAME
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_TEAM
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
                                                var notificationToken: String?,
                                                var code: String? = null,
                                                var isNotificationActive: Boolean = true,
                                                var isVerified: Boolean = false,
                                                var submittedArenas: Number = 0,
                                                var submittedPokestops: Number = 0,
                                                var submittedQuests: Number = 0,
                                                var submittedRaids: Number = 0,
                                                var subscribedGeohashArenas: List<GeoHash>? = emptyList(),
                                                var subscribedGeohashPokestops: List<GeoHash>? = emptyList(),
                                                var photoURL: Uri? = null): FirebaseNode {

    override fun databasePath(): String {
        return "$USERS/$id"
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[USER_ID] = id
        data[EMAIL] = email
        data[NOTIFICATION_ACTIVE] = isNotificationActive
        notificationToken?.let { data[NOTIFICATION_TOKEN] = it }

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
            val notificationActive = dataSnapshot.child(NOTIFICATION_ACTIVE).value as? Boolean

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

            val subscribedGeohashPokestops = (dataSnapshot.child(SUBSCRIBED_GEOHASH_POKESTOPS) as? DataSnapshot)?.children?.mapNotNull { child -> child.key?.let { GeoHash(it) } }?.toList()
            val subscribedGeohashArenas = (dataSnapshot.child(SUBSCRIBED_GEOHASH_ARENAS) as? DataSnapshot)?.children?.mapNotNull { child -> child.key?.let { GeoHash(it) } }?.toList()

//            Log.v(TAG, "id: $id, name: $name, email: $email, team: $team, level: $level, notificationToken: $notificationToken, submittedArenas: $submittedArenas, submittedPokestops: $submittedPokestops")

            if (id != null && name != null && email != null && team != null && level != null && notificationToken != null) {
                val user = new(id, email, name, team, level, notificationToken)

                // optionals
                code?.let { user.code = it }
                notificationActive?.let { user.isNotificationActive = it }

                submittedArenas?.let { user.submittedArenas = it }
                submittedPokestops?.let { user.submittedPokestops = it }
                submittedQuests?.let { user.submittedQuests = it }
                submittedRaids?.let { user.submittedRaids = it }

                subscribedGeohashPokestops?.let { user.subscribedGeohashPokestops = it }
                subscribedGeohashArenas?.let { user.subscribedGeohashArenas = it }

                return user
            }

            return null
        }

        fun new(uid: String, email: String, name: String, team: Team, level: Number, notificationToken: String? = null): FirebaseUserNode {
            return FirebaseUserNode(uid, email, name, team, level, notificationToken)
        }
    }
}