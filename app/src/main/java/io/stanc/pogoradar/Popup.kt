package io.stanc.pogoradar

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import io.stanc.pogoradar.utils.Kotlin.safeLet


object Popup {
    private val TAG = javaClass.name

    fun showInfo(context: Context?, @StringRes title: Int, @StringRes description: Int? = null) {

        context?.let {

            try {

                val builder = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setNeutralButton(R.string.popup_info_button_neutral, null)

                description?.let {
                    builder.setMessage(description)
                }

                builder.show()

            } catch (e: Exception) {
                e.printStackTrace()
            }


        } ?: run {
            Log.e(TAG, "could not show popup with title: $title, because context is null!")
        }
    }

    fun showToast(context: Context?, @StringRes description: Int) {

        context?.let {

            val toast = Toast.makeText(context, description, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

        } ?: run {
            Log.e(TAG, "Could not show toast ($description) because context: $context!")
        }
    }

    fun showToast(context: Context?, description: String?) {

        safeLet(context, description) { context, description ->

            val toast = Toast.makeText(context, description, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

        } ?: run {
            Log.e(TAG, "Could not show toast, because context: $context or description: $description!")
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