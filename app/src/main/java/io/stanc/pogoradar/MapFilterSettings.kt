package io.stanc.pogoradar

import android.content.SharedPreferences
import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import io.stanc.pogoradar.utils.addOnPropertyChanged
import java.lang.ref.WeakReference


object MapFilterSettings {
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

    val anyFilterIsActive = ObservableField<Boolean>()

    init {

        App.preferences?.let { preferences ->

            setupInitFieldValues(preferences)
            setupOnPropertiesChanged(preferences)
            checkWhetherFilterIsActive()
        }

    }

    private fun setupInitFieldValues(preferences: SharedPreferences) {
        enableArenas.set(preferences.getBoolean(MapFilterSettings::enableArenas.name, true))
        justRaidArenas.set(preferences.getBoolean(MapFilterSettings::justRaidArenas.name, false))
        justEXArenas.set(preferences.getBoolean(MapFilterSettings::justEXArenas.name, false))
        enablePokestops.set(preferences.getBoolean(MapFilterSettings::enablePokestops.name, true))
        justQuestPokestops.set(preferences.getBoolean(MapFilterSettings::justQuestPokestops.name, false))
    }

    private fun setupOnPropertiesChanged(preferences: SharedPreferences) {

        if (enableArenasCallback == null) {
            enableArenasCallback = enableArenas.addOnPropertyChanged { enableArenas ->
                preferences.edit().putBoolean(MapFilterSettings::enableArenas.name, enableArenas.get() == true)?.apply()
                checkWhetherFilterIsActive()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        }

        if (justRaidArenasCallback == null) {
            justRaidArenasCallback = justRaidArenas.addOnPropertyChanged { justRaidArenas ->
                preferences.edit().putBoolean(MapFilterSettings::justRaidArenas.name, justRaidArenas.get() == true)?.apply()
                checkWhetherFilterIsActive()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        }

        if (justEXArenasCallback == null) {
            justEXArenasCallback = justEXArenas.addOnPropertyChanged { justEXArenas ->
                preferences.edit().putBoolean(MapFilterSettings::justEXArenas.name, justEXArenas.get() == true)?.apply()
                checkWhetherFilterIsActive()
                observers.forEach { it.value.get()?.onArenasVisibilityDidChange() }
            }
        }

        if (enablePokestopsCallback == null) {
            enablePokestopsCallback = enablePokestops.addOnPropertyChanged { enablePokestops ->
                preferences.edit().putBoolean(MapFilterSettings::enablePokestops.name, enablePokestops.get() == true)?.apply()
                checkWhetherFilterIsActive()
                observers.forEach {
                    it.value.get()?.onPokestopsVisibilityDidChange()
                }
            }
        }

        if (justQuestPokestopsCallback == null) {
            justQuestPokestopsCallback = justQuestPokestops.addOnPropertyChanged { justQuestPokestops ->
                preferences.edit().putBoolean(MapFilterSettings::justQuestPokestops.name, justQuestPokestops.get() == true)?.apply()
                checkWhetherFilterIsActive()
                observers.forEach {
                    it.value.get()?.onPokestopsVisibilityDidChange()
                }
            }
        }
    }

    private fun checkWhetherFilterIsActive() {

        val atLeastOnFilterIstActive = enableArenas.get() == false ||
                                                enablePokestops.get() == false ||
                                                justEXArenas.get() == true ||
                                                justRaidArenas.get() == true ||
                                                justQuestPokestops.get() == true

        Log.d(TAG, "Debug:: checkWhetherFilterIsActive($atLeastOnFilterIstActive), enableArenas: ${enableArenas.get()}, enablePokestops: ${enablePokestops.get()}, justEXArenas: ${justEXArenas.get()}, justRaidArenas: ${justRaidArenas.get()}, justQuestPokestops: ${justQuestPokestops.get()}")
        anyFilterIsActive.set(atLeastOnFilterIstActive)
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
