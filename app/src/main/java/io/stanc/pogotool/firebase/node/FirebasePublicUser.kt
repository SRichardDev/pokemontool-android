package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.USERS
import io.stanc.pogotool.firebase.DatabaseKeys.USER_CODE
import io.stanc.pogotool.firebase.DatabaseKeys.USER_LEVEL
import io.stanc.pogotool.firebase.DatabaseKeys.USER_NAME
import io.stanc.pogotool.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogotool.firebase.DatabaseKeys.USER_TEAM

data class FirebasePublicUser private constructor(
    override val id: String,
    var name: String,
    var team: Team,
    var level: Number,
    var code: String? = null): FirebaseNode {

    override fun databasePath(): String = "$USERS/$id/$USER_PUBLIC_DATA"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[USER_NAME] = name
        data[USER_TEAM] = team.toNumber()
        data[USER_LEVEL] = level
        code?.let { data[USER_CODE] = it }

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(userId: String, dataSnapshot: DataSnapshot): FirebasePublicUser? {

            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val name = dataSnapshot.child(USER_NAME).value as? String
            val team = (dataSnapshot.child(USER_TEAM).value as? Number)?.let { teamNumber ->
                Team.valueOf(teamNumber)
            }
            val code = dataSnapshot.child(USER_CODE).value as? String
            val level = dataSnapshot.child(USER_LEVEL).value as? Number

            Log.v(TAG, "userId: $userId, id: ${dataSnapshot.key}, name: $name, team: $team, level: $level, code: $code")

            if (name != null && team != null && level != null) {
                return FirebasePublicUser(userId, name, team, level, code)
            }

            return null
        }
    }
}
