package io.stanc.pogotool.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import java.lang.ref.WeakReference


class InterceptableScrollView: ScrollView {
    private val TAG = javaClass.name

    private var interceptScrollViews: HashMap<Int, WeakReference<View>> = HashMap()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun addInterceptScrollView(view: View) {
        interceptScrollViews[view.hashCode()] = WeakReference(view)
    }

    fun removeInterceptScrollView(view: View) {
        interceptScrollViews.remove(view.hashCode())
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        for (view in interceptScrollViews.values) {
            view.get()?.let {
                if (isTouchWithinView(event.rawX, event.rawY, it)) {
                    return false
                }
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    private fun isTouchWithinView(touchX: Float, touchY: Float, view: View): Boolean {
        val locationOnScreen = intArrayOf(0, 0)
        view.getLocationOnScreen(locationOnScreen)

        return touchX < locationOnScreen[0] + view.width &&
                touchX > locationOnScreen[0] &&
                touchY < locationOnScreen[1] + view.height &&
                touchY > locationOnScreen[1]
    }
}