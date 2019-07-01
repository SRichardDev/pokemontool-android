package io.stanc.pogotool

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.annotation.StringRes

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onTerminate() {
        appContext = null
        super.onTerminate()
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
    }
}