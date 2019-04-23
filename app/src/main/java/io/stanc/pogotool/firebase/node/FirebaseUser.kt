package io.stanc.pogotool.firebase.node

import android.net.Uri
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_NOTIFICATION_TOKEN
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USERS

data class FirebaseUser(override var id: String,
                        var name: String?,
                        var email: String?,
                        var isVerified: Boolean = false,
                        var notificationToken: String? = null,
                        var photoURL: Uri? = null): FirebaseNode {

    override fun databasePath(): String {
        return "$DATABASE_USERS/$id"
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        notificationToken?.let { data[DATABASE_NOTIFICATION_TOKEN] = it }
        return data
    }
}