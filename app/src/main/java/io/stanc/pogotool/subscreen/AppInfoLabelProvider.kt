package io.stanc.pogotool.subscreen

import android.view.View
import android.widget.TextView
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.map.MapSettings
import java.lang.ref.WeakReference

class AppInfoLabelController(layout: View) {

    private val layout = WeakReference(layout)
    private val connectionTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_connection_error))
    private val arenaFilterTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_arena_filter))
    private val pokestopFilterTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_pokestop_filter))
    private val subscriptionsEnableTextView = WeakReference(layout.findViewById<TextView>(R.id.app_info_label_subscriptions_enable))

    fun start() {
        FirebaseServer.addConnectionListener(connectionListener)
        MapSettings.addObserver(mapSettingsObserver)
    }

    fun stop() {
        FirebaseServer.removeConnectionListener(connectionListener)
        MapSettings.removeObserver(mapSettingsObserver)
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

    private val mapSettingsObserver = object : MapSettings.MapSettingObserver {

        override fun onArenasVisibilityDidChange() {
            val filterActivated =  MapSettings.enableArenas.get() == false || MapSettings.justEXArenas.get() == true || MapSettings.justRaidArenas.get() == true
            if (filterActivated) {
                arenaFilterTextView.get()?.visibility = View.VISIBLE
                arenaFilterTextView.get()?.text = App.geString(R.string.info_label_arena_filter)
            } else {
                arenaFilterTextView.get()?.visibility = View.GONE
            }
            updateLayout()
        }

        override fun onPokestopsVisibilityDidChange() {
            val filterActivated =  MapSettings.enablePokestops.get() == false || MapSettings.justQuestPokestops.get() == true
            if (filterActivated) {
                pokestopFilterTextView.get()?.visibility = View.VISIBLE
                pokestopFilterTextView.get()?.text = App.geString(R.string.info_label_pokestop_filter)
            } else {
                pokestopFilterTextView.get()?.visibility = View.GONE
            }
            updateLayout()
        }

        override fun onSubscriptionsEnableDidChange() {
            if (MapSettings.enableSubscriptions.get() == false) {
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