package io.stanc.pogoradar

import android.content.SharedPreferences
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import io.stanc.pogoradar.utils.addOnPropertyChanged
import java.lang.ref.WeakReference


object AppSettings {
    private val TAG = javaClass.name

    private var enableArenasCallback: Observable.OnPropertyChangedCallback? = null
    private var justRaidArenasCallback: Observable.OnPropertyChangedCallback? = null
    private var justEXArenasCallback: Observable.OnPropertyChangedCallback? = null
    private var enablePokestopsCallback: Observable.OnPropertyChangedCallback? = null
    private var justQuestPokestopsCallback: Observable.OnPropertyChangedCallback? = null

    val enableArenas = ObservableField<Boolean>()
    val justRaidArenas = ObservableField<Boolean>()
    val justEXArenas = ObservableField<Boolean>()
    val enablePokestops = ObservableField<Boolean>()
    val justQuestPokestops = ObservableField<Boolean>()

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
    }

    private fun setupOnPropertiesChanged(preferences: SharedPreferences) {

        if (enableArenasCallback == null) {
            enableArenasCallback = enableArenas.addOnPropertyChanged { enableArenas ->
                preferences.edit().putBoolean(AppSettings::enableArenas.name, enableArenas.get() == true)?.apply()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        }

        if (justRaidArenasCallback == null) {
            justRaidArenasCallback = justRaidArenas.addOnPropertyChanged { justRaidArenas ->
                preferences.edit().putBoolean(AppSettings::justRaidArenas.name, justRaidArenas.get() == true)?.apply()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        }

        if (justEXArenasCallback == null) {
            justEXArenasCallback = justEXArenas.addOnPropertyChanged { justEXArenas ->
                preferences.edit().putBoolean(AppSettings::justEXArenas.name, justEXArenas.get() == true)?.apply()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        }

        if (enablePokestopsCallback == null) {
            enablePokestopsCallback = enablePokestops.addOnPropertyChanged { enablePokestops ->
                preferences.edit().putBoolean(AppSettings::enablePokestops.name, enablePokestops.get() == true)?.apply()
                observers.forEach {
                    it.value.get()?.onPokestopsVisibilityDidChange()
                }
            }
        }

        if (justQuestPokestopsCallback == null) {
            justQuestPokestopsCallback = justQuestPokestops.addOnPropertyChanged { justQuestPokestops ->
                preferences.edit().putBoolean(AppSettings::justQuestPokestops.name, justQuestPokestops.get() == true)?.apply()
                observers.forEach {
                    it.value.get()?.onPokestopsVisibilityDidChange()
                }
            }
        }
    }

    /**
     * observer
     */

    interface MapSettingObserver {
        fun onArenasVisibilityDidChange()
        fun onPokestopsVisibilityDidChange()
    }

    private val observers = HashMap<Int, WeakReference<MapSettingObserver>>()

    fun addObserver(observer: MapSettingObserver) {
        val weakObserver = WeakReference(observer)
        observers[observer.hashCode()] = weakObserver
        observer.onArenasVisibilityDidChange()
        observer.onPokestopsVisibilityDidChange()
    }

    fun removeObserver(observer: MapSettingObserver) {
        observers.remove(observer.hashCode())
    }
}
