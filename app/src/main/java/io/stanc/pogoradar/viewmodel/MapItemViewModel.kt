package io.stanc.pogoradar.viewmodel

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogoradar.utils.SegmentedControlView
import io.stanc.pogoradar.utils.addOnPropertyChanged

class MapItemViewModel: ViewModel() {
    private val TAG = javaClass.name

    enum class Type {
        Arena,
        Pokestop
    }

    private var typeChangedCalback: Observable.OnPropertyChangedCallback? = null
    private var nameChangedCalback: Observable.OnPropertyChangedCallback? = null

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
        typeChangedCalback = type.addOnPropertyChanged {
            updateNameWithType()
        }

        nameChangedCalback = name.addOnPropertyChanged {
            updateNameWithType()
        }
    }

    override fun onCleared() {
        typeChangedCalback?.let { type.removeOnPropertyChangedCallback(it) }
        nameChangedCalback?.let { name.removeOnPropertyChangedCallback(it) }
        super.onCleared()
    }

    fun reset() {
        position.set(null)
        type.set(Type.Pokestop)
        isEx.set(false)
        name.set(null)
        nameWithType.set(null)
    }

    private fun updateNameWithType() {
        nameWithType.set("${type.get()?.name}: ${name.get()}")
    }
}