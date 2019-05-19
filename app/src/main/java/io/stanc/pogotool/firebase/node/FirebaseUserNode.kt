package io.stanc.pogotool.firebase.node

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.EMAIL
import io.stanc.pogotool.firebase.DatabaseKeys.USERS
import io.stanc.pogotool.firebase.DatabaseKeys.NOTIFICATION_TOKEN
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_POKESTOPS
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_Quests
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_RAIDS
import io.stanc.pogotool.firebase.DatabaseKeys.USER_ID
import io.stanc.pogotool.firebase.DatabaseKeys.USER_NAME
import io.stanc.pogotool.firebase.DatabaseKeys.USER_TEAM


data class FirebaseUserNode(override var id: String,
                            var trainerName: String,
                            var email: String,
                            var team: Number,
                            var notificationToken: String? = null,
                            var isVerified: Boolean? = false,
                            var submittedArenas: Number = 0,
                            var submittedPokestops: Number = 0,
                            var submittedQuests: Number = 0,
                            var submittedRaids: Number = 0,
                            var photoURL: Uri? = null): FirebaseNode {

    override fun databasePath(): String {
        return "$USERS/$id"
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[USER_ID] = id
        data[USER_NAME] = trainerName
        data[EMAIL] = email
        data[USER_TEAM] = team
        notificationToken?.let { data[NOTIFICATION_TOKEN] = it }

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseUserNode? {

            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val trainerName = dataSnapshot.child(USER_NAME).value as? String
            val email = dataSnapshot.child(EMAIL).value as? String
            val team = dataSnapshot.child(USER_TEAM).value as? Number
            val notificationToken = dataSnapshot.child(NOTIFICATION_TOKEN).value as? String

            val submittedArenas =
                (dataSnapshot.child(SUBMITTED_ARENAS).value as? DataSnapshot)?.childrenCount ?: kotlin.run { null }
            val submittedPokestops =
                (dataSnapshot.child(SUBMITTED_POKESTOPS).value as? DataSnapshot)?.childrenCount ?: kotlin.run { null }
            val submittedQuests =
                (dataSnapshot.child(SUBMITTED_Quests).value as? DataSnapshot)?.childrenCount ?: kotlin.run { null }
            val submittedRaids =
                (dataSnapshot.child(SUBMITTED_RAIDS).value as? DataSnapshot)?.childrenCount ?: kotlin.run { null }

            Log.v(
                TAG,
                "id: $id, trainerName: $trainerName, email: $email, team: $team, notificationToken: $notificationToken, submittedArenas: $submittedArenas, submittedPokestops: $submittedPokestops"
            )

            if (id != null && trainerName != null && email != null && team != null) {
                val user = FirebaseUserNode(id, trainerName, email, team)

                notificationToken?.let { user.notificationToken = it }
                submittedArenas?.let { user.submittedArenas = it }
                submittedPokestops?.let { user.submittedArenas = it }
                submittedQuests?.let { user.submittedQuests = it }
                submittedRaids?.let { user.submittedRaids = it }

                return user
            }

            return null
        }
    }
}