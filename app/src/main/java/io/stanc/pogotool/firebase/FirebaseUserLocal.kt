package io.stanc.pogotool.firebase

import android.net.Uri

data class FirebaseUserLocal(var name: String? = null,
                             var email: String? = null,
                             var isVerified: Boolean = false,
                             var id: String? = null,
                             var notificationToken: String? = null,
                             var photoURL: Uri? = null)