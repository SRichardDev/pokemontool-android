package io.stanc.pogotool

import android.content.SharedPreferences
import android.databinding.Observable
import android.databinding.ObservableField
import java.lang.ref.WeakReference


object AppSettings {
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
        enableArenas.set(preferences.getBoolean(AppSettings::enableArenas.name, true))
        justRaidArenas.set(preferences.getBoolean(AppSettings::justRaidArenas.name, false))
        justEXArenas.set(preferences.getBoolean(AppSettings::justEXArenas.name, false))
        enablePokestops.set(preferences.getBoolean(AppSettings::enablePokestops.name, true))
        justQuestPokestops.set(preferences.getBoolean(AppSettings::justQuestPokestops.name, false))
        enableSubscriptions.set(preferences.getBoolean(AppSettings::enableSubscriptions.name, true))
    }

    private fun setupOnPropertiesChanged(preferences: SharedPreferences) {

        enableArenas.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(AppSettings::enableArenas.name, enableArenas.get() == true)?.apply()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        })

        justRaidArenas.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(AppSettings::justRaidArenas.name, justRaidArenas.get() == true)?.apply()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        })

        justEXArenas.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(AppSettings::justEXArenas.name, justEXArenas.get() == true)?.apply()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        })

        enablePokestops.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(AppSettings::enablePokestops.name, enablePokestops.get() == true)?.apply()
                observers.forEach {
                    it.value.get()?.onPokestopsVisibilityDidChange()
                }
            }
        })

        justQuestPokestops.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(AppSettings::justQuestPokestops.name, justQuestPokestops.get() == true)?.apply()
                observers.forEach {
                    it.value.get()?.onPokestopsVisibilityDidChange()
                }
            }
        })

        enableSubscriptions.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                preferences.edit().putBoolean(AppSettings::enableSubscriptions.name, enableSubscriptions.get() == true)?.apply()
                observers.forEach {
                    it.value.get()?.onSubscriptionsEnableDidChange()
                }
            }
        })
    }

    /**
     * observer
     */

    interface MapSettingObserver {
        fun onArenasVisibilityDidChange()
        fun onPokestopsVisibilityDidChange()
        fun onSubscriptionsEnableDidChange() {}
    }

    private val observers = HashMap<Int, WeakReference<MapSettingObserver>>()

    fun addObserver(observer: MapSettingObserver) {
        val weakObserver = WeakReference(observer)
        observers[observer.hashCode()] = weakObserver
        observer.onArenasVisibilityDidChange()
        observer.onPokestopsVisibilityDidChange()
        observer.onSubscriptionsEnableDidChange()
    }

    fun removeObserver(observer: MapSettingObserver) {
        observers.remove(observer.hashCode())
    }
}