package io.stanc.pogotool.map

import android.content.SharedPreferences
import android.databinding.Observable
import android.databinding.ObservableField
import io.stanc.pogotool.App


object MapSettings {
    private val TAG = javaClass.name

    val enableArenas = ObservableField<Boolean>()
    val justRaidArenas = ObservableField<Boolean>()
    val justEXArenas = ObservableField<Boolean>()
    val enablePokestops = ObservableField<Boolean>()
    val justQuestPokestops = ObservableField<Boolean>()
    val enableSubscriptions = ObservableField<Boolean>()

    init {

        App.preferences?.let { preferences ->

            setupInitFieldValues(preferences)
            setupOnPropertiesChanged(preferences)
        }
    }

    private fun setupInitFieldValues(preferences: SharedPreferences) {
        enableArenas.set(preferences.getBoolean(::enableArenas.name, true))
        justRaidArenas.set(preferences.getBoolean(::justRaidArenas.name, false))
        justEXArenas.set(preferences.getBoolean(::justEXArenas.name, false))
        enablePokestops.set(preferences.getBoolean(::enablePokestops.name, true))
        justQuestPokestops.set(preferences.getBoolean(::justQuestPokestops.name, false))
        enableSubscriptions.set(preferences.getBoolean(::enableSubscriptions.name, true))
    }

    private fun setupOnPropertiesChanged(preferences: SharedPreferences) {

        enableArenas.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(::enableArenas.name, enableArenas.get() == true)?.apply()
            }
        })

        justRaidArenas.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(::justRaidArenas.name, justRaidArenas.get() == true)?.apply()
            }
        })

        justEXArenas.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(::justEXArenas.name, justEXArenas.get() == true)?.apply()
            }
        })

        enablePokestops.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(::enablePokestops.name, enablePokestops.get() == true)?.apply()
            }
        })

        justQuestPokestops.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(::justQuestPokestops.name, justQuestPokestops.get() == true)?.apply()
            }
        })

        enableSubscriptions.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(::enableSubscriptions.name, enableSubscriptions.get() == true)?.apply()
            }
        })
    }
}