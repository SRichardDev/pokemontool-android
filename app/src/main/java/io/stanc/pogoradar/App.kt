package io.stanc.pogoradar

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.core.content.pm.PackageInfoCompat

class App: Application() {
    private val TAG = javaClass.name

    override fun onCreate() {
        super.onCreate()
        appContext = this
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        try {
            val packageInfo = baseContext.packageManager.getPackageInfo(baseContext.packageName, 0)
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
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
        var versionCode: Long? = null
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