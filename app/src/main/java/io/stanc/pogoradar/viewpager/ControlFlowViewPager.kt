package io.stanc.pogoradar.viewpager

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager
import android.view.MotionEvent



class ControlFlowViewPager(context: Context, attrs: AttributeSet? = null): ViewPager(context, attrs) {

    var swipeEnabled: Boolean = true

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        return if (swipeEnabled) {
            super.onInterceptTouchEvent(event)
        } else {
            false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        return if (swipeEnabled) {
            super.onTouchEvent(event)
        } else {
            false
        }
    }
}