package io.stanc.pogoradar.subscreen

import android.view.View
import android.widget.TextView
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.AppSettings
import java.lang.ref.WeakReference

class AppInfoLabelController(layout: View) {

    private val layout = WeakReference(layout)
    private val connectionTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_connection_error))
    private val arenaFilterTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_arena_filter))
    private val pokestopFilterTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_pokestop_filter))
    private val subscriptionsEnableTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_subscriptions_enable))

    fun start() {
        FirebaseServer.addConnectionListener(connectionListener)
        AppSettings.addObserver(mapSettingsObserver)
    }

    fun stop() {
        FirebaseServer.removeConnectionListener(connectionListener)
        AppSettings.removeObserver(mapSettingsObserver)
    }

    private val connectionListener = object : (Boolean) -> Unit {
        override fun invoke(connected: Boolean) {
            if (connected) {
                connectionTextView.get()?.visibility = View.GONE
            } else {
                connectionTextView.get()?.visibility = View.VISIBLE
                connectionTextView.get()?.text = App.geString(R.string.info_label_connection_lost)
            }
            updateLayout()
        }
    }

    private val mapSettingsObserver = object : AppSettings.MapSettingObserver {

        override fun onArenasVisibilityDidChange() {
            val filterActivated =  AppSettings.enableArenas.get() == false || AppSettings.justEXArenas.get() == true || AppSettings.justRaidArenas.get() == true
            if (filterActivated) {
                arenaFilterTextView.get()?.visibility = View.VISIBLE
                arenaFilterTextView.get()?.text = App.geString(R.string.info_label_arena_filter)
            } else {
                arenaFilterTextView.get()?.visibility = View.GONE
            }
            updateLayout()
        }

        override fun onPokestopsVisibilityDidChange() {
            val filterActivated =  AppSettings.enablePokestops.get() == false || AppSettings.justQuestPokestops.get() == true
            if (filterActivated) {
                pokestopFilterTextView.get()?.visibility = View.VISIBLE
                pokestopFilterTextView.get()?.text = App.geString(R.string.info_label_pokestop_filter)
            } else {
                pokestopFilterTextView.get()?.visibility = View.GONE
            }
            updateLayout()
        }

        override fun onSubscriptionsEnableDidChange() {
            if (AppSettings.enableSubscriptions.get() == false) {
                subscriptionsEnableTextView.get()?.visibility = View.VISIBLE
                subscriptionsEnableTextView.get()?.text = App.geString(R.string.info_label_subscriptions_disabled)
            } else {
                subscriptionsEnableTextView.get()?.visibility = View.GONE
            }
            updateLayout()
        }
    }

    private fun updateLayout() {
        if (connectionTextView.get()?.visibility == View.GONE &&
            arenaFilterTextView.get()?.visibility == View.GONE &&
            pokestopFilterTextView.get()?.visibility == View.GONE &&
            subscriptionsEnableTextView.get()?.visibility == View.GONE) {

            layout.get()?.visibility = View.GONE
        } else {
            layout.get()?.visibility = View.VISIBLE
        }
    }
}