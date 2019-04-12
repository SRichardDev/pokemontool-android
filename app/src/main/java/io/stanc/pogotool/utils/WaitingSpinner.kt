package io.stanc.pogotool.utils

import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import java.lang.ref.WeakReference


object WaitingSpinner {

    // https://stackoverflow.com/questions/45373007/progressdialog-is-deprecated-what-is-the-alternate-one-to-use
    private var progressBarView: WeakReference<View>? = null
    private var progressBarTitle: WeakReference<TextView>? = null
    private var window: WeakReference<Window>? = null

    fun initialize(progressBarView: View, progressBarTextView: TextView, window: Window) {
        WaitingSpinner.progressBarView = WeakReference(progressBarView)
        WaitingSpinner.progressBarTitle = WeakReference(progressBarTextView)
        WaitingSpinner.window = WeakReference(window)
    }

    fun showProgress(@StringRes textId: Int) {
        progressBarView?.get()?.let { it.visibility = View.VISIBLE }
        KotlinUtils.safeLet(progressBarTitle?.get(), window?.get()?.context) { title, context ->
            title.text = context.getText(textId)
        }
        window?.get()?.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun hideProgress() {
        progressBarView?.get()?.let { it.visibility = View.GONE }
        window?.get()?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}