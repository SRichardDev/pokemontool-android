package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.Observable
import android.databinding.ObservableField
import com.google.android.gms.maps.model.LatLng

class MapItemViewModel: ViewModel() {

    val position = ObservableField<LatLng>()
    val type = ObservableField<Type>(Type.Pokestop)
    // TODO: remove this field
    val debug_typeIsArena = ObservableField<Boolean>(false)
    val isEx = ObservableField<Boolean?>()
    val name = ObservableField<String>()
    val nameWithType = ObservableField<String>()

    init {
        debug_typeIsArena.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (debug_typeIsArena.get() == true) {
                    type.set(Type.Arena)
                } else {
                    type.set(Type.Pokestop)
                }
            }
        })

        type.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                updateNameWithType()
            }
        })

        name.addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                updateNameWithType()
            }
        })
    }

    private fun updateNameWithType() {
        nameWithType.set("${type.get()?.name}: ${name.get()}")
    }

    enum class Type {
        Arena,
        Pokestop
    }
}