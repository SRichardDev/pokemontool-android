package io.stanc.pogotool

import android.app.Application
import android.content.Context
import android.support.annotation.StringRes

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {

        private var appContext: Context? = null

        fun geString(@StringRes stringResId: Int): String? {
            return appContext?.resources?.getString(stringResId)
        }
    }
}