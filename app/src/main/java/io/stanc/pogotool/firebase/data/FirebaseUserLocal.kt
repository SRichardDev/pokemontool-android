package io.stanc.pogotool.firebase.data

import android.net.Uri
import io.stanc.pogotool.firebase.FirebaseServer.DATABASE_NOTIFICATION_TOKEN
import io.stanc.pogotool.firebase.FirebaseServer.DATABASE_USERS

data class FirebaseUserLocal(var name: String? = null,
                             var email: String? = null,
                             var isVerified: Boolean = false,
                             var id: String? = null,
                             var notificationToken: String? = null,
                             var photoURL: Uri? = null): FirebaseData {

    override fun databasePath(): String {
        return id?.let {
            "$DATABASE_USERS/$it"
        } ?: kotlin.run { "" }
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        notificationToken?.let { data[DATABASE_NOTIFICATION_TOKEN] = it }
        return data
    }

}