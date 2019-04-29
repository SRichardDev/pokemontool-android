package io.stanc.pogotool.firebase.node

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USER_NOTIFICATION_TOKEN
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USERS

data class FirebaseUserNode(override var id: String,
                            var trainerName: String,
                            var email: String,
                            var team: Number,
                            var notificationToken: String? = null,
                            var isVerified: Boolean? = false,
                            var submittedArenas: Number = 0,
                            var submittedPokestops: Number = 0,
                            var photoURL: Uri? = null): FirebaseNode {

    override fun databasePath(): String {
        return "$DATABASE_USERS/$id"
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["id"] = id
        data["trainerName"] = trainerName
        data["email"] = email
        data["team"] = team
        notificationToken?.let { data[DATABASE_USER_NOTIFICATION_TOKEN] = it }

//        isVerified ?
//        submittedArenas?.let { data["submittedArenas"] = it }
//        submittedPokestops?.let { data["submittedPokestops"] = it }
//        photoURL ?

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseUserNode? {

            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val trainerName = dataSnapshot.child("trainerName").value as? String
            val email = dataSnapshot.child("email").value as? String
            val team = dataSnapshot.child("team").value as? Number
            val notificationToken = dataSnapshot.child("notificationToken").value as? String

            val submittedArenas =
                (dataSnapshot.child("submittedArenas").value as? DataSnapshot)?.childrenCount ?: kotlin.run { null }
            val submittedPokestops =
                (dataSnapshot.child("submittedPokestops").value as? DataSnapshot)?.childrenCount ?: kotlin.run { null }

            Log.v(
                TAG,
                "id: $id, trainerName: $trainerName, email: $email, team: $team, notificationToken: $notificationToken, submittedArenas: $submittedArenas, submittedPokestops: $submittedPokestops"
            )

            if (id != null && trainerName != null && email != null && team != null) {
                val user = FirebaseUserNode(id, trainerName, email, team)

                notificationToken?.let { user.notificationToken = it }
                submittedArenas?.let { user.submittedArenas = it }
                submittedPokestops?.let { user.submittedArenas = it }

                return user
            }

            return null
        }
    }
}