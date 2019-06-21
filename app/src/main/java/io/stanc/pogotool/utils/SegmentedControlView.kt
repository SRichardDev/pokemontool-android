package io.stanc.pogotool.utils

import android.content.Context
import android.databinding.BindingAdapter
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.stanc.pogotool.R
import java.lang.ref.WeakReference

@BindingAdapter("onSelectionChangeListener")
fun setOnSelectionChangeListener(view: SegmentedControlView, listener: SegmentedControlView.OnSelectionChangeListener?) {
    listener?.let { view.setOnSelectionChangeListener(listener) }
}

@BindingAdapter("textLeft")
fun setTextLeft(view: SegmentedControlView, text: String?) {
    text?.let { view.setSegment(text, SegmentedControlView.Selection.LEFT) }
}

@BindingAdapter("textMiddle")
fun setTextMiddle(view: SegmentedControlView, text: String?) {
    text?.let { view.setSegment(text, SegmentedControlView.Selection.MIDDLE) }
}

@BindingAdapter("textRight")
fun setTextRight(view: SegmentedControlView, text: String?) {
    text?.let { view.setSegment(text, SegmentedControlView.Selection.RIGHT) }
}

class SegmentedControlView: LinearLayout {
    private val TAG = javaClass.name

    enum class Selection {
        LEFT,
        MIDDLE,
        RIGHT
    }

    interface OnSelectionChangeListener {
        fun onSelectionChange(selection: Selection)
    }

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    private val buttonLeft: TextView
    private val buttonMiddle: TextView
    private val buttonRight: TextView
    private var onSelectionChangeListener: WeakReference<OnSelectionChangeListener>? = null

    var selection: Selection = Selection.LEFT
        set(value) {
            field = value
            selectButton(value)
            onSelectionChangeListener?.get()?.onSelectionChange(value)
        }

    init {
        View.inflate(context, R.layout.layout_segmented_control, this)

        buttonLeft = this.findViewById(R.id.button_left)
        buttonLeft.setOnClickListener { selection = Selection.LEFT }

        buttonMiddle = this.findViewById(R.id.button_middle)
        buttonMiddle.setOnClickListener { selection = Selection.MIDDLE }

        buttonRight = this.findViewById(R.id.button_right)
        buttonRight.setOnClickListener { selection = Selection.RIGHT }

        selectButton(Selection.LEFT)
    }

    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        Log.i(TAG, "Debug:: setOnSelectionChangeListener(listener: $listener)")
        onSelectionChangeListener = WeakReference(listener)
    }

    fun setSegment(text: String, selection: Selection) {
        Log.i(TAG, "Debug:: setSegment(text: $text, selection: ${selection.name})")
        when(selection) {
            Selection.LEFT -> buttonLeft.text = text
            Selection.MIDDLE -> buttonMiddle.text = text
            Selection.RIGHT -> buttonRight.text = text
        }

        changeVisibility()
    }

    private fun selectButton(selection: Selection) {
        Log.i(TAG, "Debug:: selectButton(selection: ${selection.name})")
        when(selection) {
            Selection.LEFT -> {
                buttonLeft.isSelected = true
                buttonMiddle.isSelected = false
                buttonRight.isSelected = false
            }
            Selection.MIDDLE -> {
                buttonLeft.isSelected = false
                buttonMiddle.isSelected = true
                buttonRight.isSelected = false
            }
            Selection.RIGHT -> {
                buttonLeft.isSelected = false
                buttonMiddle.isSelected = false
                buttonRight.isSelected = true
            }
        }
    }

    private fun changeVisibility() {
        buttonLeft.visibility = if(buttonLeft.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        buttonMiddle.visibility = if(buttonMiddle.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        buttonRight.visibility = if(buttonRight.text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
}