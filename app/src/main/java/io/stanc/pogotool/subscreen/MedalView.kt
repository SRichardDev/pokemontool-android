package io.stanc.pogotool.subscreen

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import io.stanc.pogotool.R


@BindingAdapter("number")
fun setNumber(view: MedalView, number: Int?) {
    number?.let { view.setNumber(number) }
}

@BindingAdapter("medalDescription")
fun setMedalDescription(view: MedalView, text: String?) {
    text?.let { view.setMedalDescription(text) }
}

class MedalView: ConstraintLayout {
    private val TAG = javaClass.name

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    private val medalImage: ImageView
    private val numberTextView: TextView
    private val descriptionTextView: TextView

    init {
        View.inflate(context, R.layout.layout_medal, this)

        medalImage = this.findViewById(R.id.medal_imageview)
        numberTextView = this.findViewById(R.id.medal_textView_number)
        descriptionTextView = this.findViewById(R.id.medal_textView_description)

        updateMedal()
    }

    fun setNumber(number: Int) {
        numberTextView.text = number.toString()
        updateMedal()
    }

    fun setMedalDescription(text: String) {
        descriptionTextView.text = text
    }

    private fun updateMedal() {

        try {
            val number = Integer.parseInt(numberTextView.text.toString())
            when(number) {
                0 -> medalImage.setImageDrawable(context.getDrawable(R.drawable.icon_medal_none_96dp))
                in 1..49 -> medalImage.setImageDrawable(context.getDrawable(R.drawable.icon_medal_broze_96dp))
                in 50..99 -> medalImage.setImageDrawable(context.getDrawable(R.drawable.icon_medal_silver_96dp))
                in 100..1000 -> medalImage.setImageDrawable(context.getDrawable(R.drawable.icon_medal_gold_96dp))
                else -> medalImage.setImageDrawable(context.getDrawable(R.drawable.icon_medal_platinum_96dp))
            }

        } catch(e: NumberFormatException) {
            Log.e(TAG, "could not convert ${numberTextView.text} to a number.")
        }
    }
}