package io.stanc.pogotool.firebase

import android.net.Uri

data class FirebaseUserLocal(var name: String?,
                             var email: String?,
                             var photoURL: Uri? = null,
                             var isVerified: Boolean)