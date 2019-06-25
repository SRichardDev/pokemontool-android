package io.stanc.pogotool

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes

object Popup {
    private val TAG = javaClass.name

    fun showInfo(context: Context?, @StringRes title: Int, description: Int? = null) {

        context?.let {

            val builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setNeutralButton(R.string.popup_info_button_neutral, null)

            description?.let {
                builder.setMessage(description)
            }

            builder.show()

        } ?: kotlin.run {
            Log.e(TAG, "could not show popup with title: $title, because context is null!")
        }
    }

    // TODO: text parameter should be both, @StringRes(Int) or String !
//    fun showDialog(context: Context, @StringRes title: Int, @StringRes actionButtonText: Int, @StringRes cancelButtonText: Int, onActionButtonClicked: () -> Unit, @StringRes description: Int? = null, iconDrawable: Drawable? = null) {
//
//        val builder = AlertDialog.Builder(context)
//            .setTitle(title)
//            .setPositiveButton(actionButtonText) { _ , _ ->
//                onActionButtonClicked()
//            }
//            .setNegativeButton(cancelButtonText, null)
//
//        description?.let {
//            builder.setMessage(description)
//        }
//
//        iconDrawable?.let {
//            builder.setIcon(it)
//        }
//
//        builder.show()
//    }
}