package io.stanc.pogotool

import android.view.View
import android.view.Window
import android.view.WindowManager
import java.lang.ref.WeakReference


object WaitingSpinner {

    // https://stackoverflow.com/questions/45373007/progressdialog-is-deprecated-what-is-the-alternate-one-to-use
    private var progressBarView: WeakReference<View>? = null
    private var window: WeakReference<Window>? = null

    fun initialize(progressBarView: View, window: Window) {
        this.progressBarView = WeakReference(progressBarView)
        this.window = WeakReference(window)
    }

    fun showProgress() {
        progressBarView?.get()?.let { it.visibility = View.VISIBLE }
        window?.get()?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun hideProgress() {
        progressBarView?.get()?.let { it.visibility = View.GONE }
        window?.get()?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}