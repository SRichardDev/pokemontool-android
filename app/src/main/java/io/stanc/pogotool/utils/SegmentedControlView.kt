package io.stanc.pogotool.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.util.StateSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import io.stanc.pogotool.R
import java.lang.ref.WeakReference


@BindingAdapter("onSelectionChangeListener")
fun setOnSelectionChangeListener(view: SegmentedControlView, listener: SegmentedControlView.OnSelectionChangeListener?) {
    listener?.let { view.setOnSelectionChangeListener(listener) }
}

@BindingAdapter("startSelection")
fun setStartSelection(view: SegmentedControlView, selection: SegmentedControlView.Selection?) {
    selection?.let { view.selection = selection }
}

@BindingAdapter("startSelection")
fun setStartSelection(view: SegmentedControlView, text: String?) {
    text?.let { view.setSelectionWithText(it) }
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

@BindingAdapter("selectionColor")
fun setSelectionColor(view: SegmentedControlView, @ColorRes color: Int?) {
    color?.let { view.setSelectionColor(color) }
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
    @ColorRes
    private var selectionColor: Int = R.color.segmentedControlSelected

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

        updateUI()
        selectButton(selection)
    }

    fun setOnSelectionChangeListener(listener: OnSelectionChangeListener) {
        onSelectionChangeListener = WeakReference(listener)
    }

    fun setSegment(text: String, selection: Selection) {
        when(selection) {
            Selection.LEFT -> buttonLeft.text = text
            Selection.MIDDLE -> buttonMiddle.text = text
            Selection.RIGHT -> buttonRight.text = text
        }
        changeVisibility()
    }

    fun setSelectionWithText(text: String) {
        when(text) {
            buttonLeft.text -> selection = Selection.LEFT
            buttonMiddle.text -> selection = Selection.MIDDLE
            buttonRight.text -> selection = Selection.RIGHT
        }
    }

    fun setSelectionColor(@ColorRes color: Int) {
        this.selectionColor = color
        updateUI()
    }

    private fun updateUI() {

        buttonLeft.background = buttonBackgroundSelector(selectionColor)
        buttonLeft.setTextColor(buttonTextSelector(selectionColor))

        buttonMiddle.background = buttonBackgroundSelector(selectionColor)
        buttonMiddle.setTextColor(buttonTextSelector(selectionColor))

        buttonRight.background = buttonBackgroundSelector(selectionColor)
        buttonRight.setTextColor(buttonTextSelector(selectionColor))

        this.background = shapeDrawable(selectionColor)
    }

    private fun buttonBackgroundSelector(@ColorRes selectionColor: Int): StateListDrawable {
        val drawable = StateListDrawable()
        drawable.addState(intArrayOf(android.R.attr.state_selected), shapeDrawable(selectionColor))
        drawable.addState(StateSet.WILD_CARD, shapeDrawable(R.color.white))
        return drawable
    }

    private fun buttonTextSelector(@ColorRes selectionColor: Int): ColorStateList {
        return ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), StateSet.WILD_CARD),
            intArrayOf(ContextCompat.getColor(context, R.color.white), ContextCompat.getColor(context, selectionColor))
        )
    }

    private fun shapeDrawable(@ColorRes color: Int): ShapeDrawable {
        val drawable = ShapeDrawable()
        val outerRadius = floatArrayOf(10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f)
        drawable.shape = RoundRectShape(outerRadius, null, null)
        drawable.paint.color = ContextCompat.getColor(context, color)
        return drawable
    }

    private fun selectButton(selection: Selection) {
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