package io.stanc.pogoradar.viewmodel

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogoradar.utils.SegmentedControlView

class MapItemViewModel: ViewModel() {
    private val TAG = javaClass.name

    enum class Type {
        Arena,
        Pokestop
    }

    val position = ObservableField<LatLng>()
    val type = ObservableField<Type>(Type.Pokestop)
    val isEx = ObservableField<Boolean?>()
    val name = ObservableField<String>()
    val nameWithType = ObservableField<String>()

    fun onSelectedStateDidChange(selection: SegmentedControlView.Selection) {
        when(selection) {
            SegmentedControlView.Selection.LEFT -> type.set(Type.Pokestop)
            SegmentedControlView.Selection.RIGHT -> type.set(Type.Arena)
            else -> {}
        }
    }

    init {
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
}