package io.stanc.pogoradar

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.DimenRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import co.chatsdk.core.error.ChatSDKException
import co.chatsdk.core.session.ChatSDK
import co.chatsdk.core.session.Configuration
import co.chatsdk.firebase.FirebaseNetworkAdapter
import co.chatsdk.firebase.file_storage.FirebaseFileStorageModule
import co.chatsdk.ui.manager.BaseInterfaceAdapter
import io.stanc.pogoradar.utils.Kotlin

class App: Application() {
    private val TAG = javaClass.name

    override fun onCreate() {
        super.onCreate()
        appContext = this
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        activateStrictMode()
        setupFirebaseChat()
    }

    override fun onTerminate() {
        appContext = null
        super.onTerminate()
    }

    private fun activateStrictMode() {
        Log.i(TAG, "BUILD_TYPE: ${BuildConfig.BUILD_TYPE}")
        if (BuildConfig.DEBUG) {
            val policy = StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
            StrictMode.setVmPolicy(policy)
        }
    }

    private fun setupFirebaseChat() {

        val context = applicationContext

        try {
            // Create a new configuration
            val builder = Configuration.Builder(context)

//            // Perform any other configuration steps (optional)
//            builder.firebaseRootPath("prod")

            // Initialize the Chat SDK
            ChatSDK.initialize(builder.build(), FirebaseNetworkAdapter(), BaseInterfaceAdapter(context))

            // File storage is needed for profile image upload and image messages
            FirebaseFileStorageModule.activate()

            // Push notification module, TODO: ?
//            FirebasePushModule.activate()

            // Activate any other modules you need.
            // ...
            // Uncomment this to enable Firebase UI
            // FirebaseUIModule.activate(EmailAuthProvider.PROVIDER_ID, PhoneAuthProvider.PROVIDER_ID);

        } catch (e: ChatSDKException) {
            // Handle any exceptions
            e.printStackTrace()
        }
    }

    companion object {

        private var appContext: Context? = null

        var preferences: SharedPreferences? = null
            private set

        fun geString(@StringRes stringResId: Int, formatArg1: String? = null, formatArg2: String? = null): String? {

            return if (formatArg1 != null && formatArg2 != null) {
                appContext?.resources?.getString(stringResId, formatArg1, formatArg2)
            } else if (formatArg1 != null) {
                appContext?.resources?.getString(stringResId, formatArg1)
            } else {
                appContext?.resources?.getString(stringResId)
            }
        }

        fun getInteger(@IntegerRes dimenResId: Int): Int? {
            return appContext?.resources?.getInteger(dimenResId)
//            return appContext?.resources?.getDimension(dimenResId)?.toInt()
        }
    }
}