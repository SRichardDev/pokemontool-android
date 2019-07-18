package io.stanc.pogoradar.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import java.lang.ref.WeakReference
import android.view.Window


object SystemUtils {

    private val TAG = javaClass.name

    /**
     * Keyboard
     */

    fun showKeyboard(context: Context) {
        val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun hideKeyboard(activity: Activity) {
        activity.currentFocus?.let { view ->
            val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    fun hideKeyboardFrom(context: Context, view: View) {
        val inputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Global Keyboard listener
     */

    interface SystemObserver {
        fun onKeyboardVisibilityDidChange(isKeyboardVisible: Boolean) {}
    }

    private var weakActivity: WeakReference<Activity>? = null

    @SuppressLint("UseSparseArrays")
    private val observers = HashMap<Int, WeakReference<SystemObserver>>()

    fun addObserver(observer: SystemObserver, activity: Activity) {

        weakActivity = WeakReference(activity)
        contentView()?.let { activityContentView ->
            activityContentView.viewTreeObserver.removeOnGlobalLayoutListener(keyboardLayoutVisibilityListener)
            activityContentView.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutVisibilityListener)
        }

        val weakObserver = WeakReference(observer)
        observers[observer.hashCode()] = weakObserver
    }

    fun removeObserver(observer: SystemObserver) {
        observers.remove(observer.hashCode())
        if (observers.size == 0) {
            contentView()?.viewTreeObserver?.removeOnGlobalLayoutListener(keyboardLayoutVisibilityListener)
            weakActivity = null
        }
    }

    private val keyboardLayoutVisibilityListener = ViewTreeObserver.OnGlobalLayoutListener {
        contentView()?.let { contentView ->

            val rectangle = Rect()
            contentView.getWindowVisibleDisplayFrame(rectangle)
            val screenHeight = contentView.rootView.height

            // r.bottom is the position above soft keypad or device button.
            // If keypad is shown, the rectangle.bottom is smaller than that before.
            val keypadHeight = screenHeight - rectangle.bottom
            // 0.15 ratio is perhaps enough to determine keypad height.
            val isKeyboardNowVisible = keypadHeight > screenHeight * 0.15

            observers.forEach { it.value.get()?.onKeyboardVisibilityDidChange(isKeyboardNowVisible) }
        }
    }

    private fun contentView(): View? = weakActivity?.get()?.findViewById(Window.ID_ANDROID_CONTENT)
}