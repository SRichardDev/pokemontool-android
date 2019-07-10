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

class App: Application() {
    private val TAG = javaClass.name

    override fun onCreate() {
        super.onCreate()
        appContext = this
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        activateStrictMode()
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

    companion object {

        private var appContext: Context? = null

        var preferences: SharedPreferences? = null
            private set

        fun geString(@StringRes stringResId: Int, formatArg1: String? = null): String? {
            formatArg1?.let {
                return appContext?.resources?.getString(stringResId, formatArg1)
            } ?: run {
                return appContext?.resources?.getString(stringResId)
            }
        }

        fun getInteger(@IntegerRes dimenResId: Int): Int? {
            return appContext?.resources?.getInteger(dimenResId)
//            return appContext?.resources?.getDimension(dimenResId)?.toInt()
        }
    }
}