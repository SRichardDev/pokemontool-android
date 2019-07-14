package io.stanc.pogoradar.utils

import androidx.databinding.Observable
import androidx.databinding.ObservableField

object Observables {

    inline fun <T> dependantObservableField(vararg dependencies: Observable, defaultValue: T? = null, crossinline mapper: () -> T?) =
        object : ObservableField<T>(*dependencies) {
            override fun get(): T? {
                return mapper()
            }
        }.apply { set(defaultValue) }
}

fun <T: Observable> T.addOnPropertyChanged(callback: (T) -> Unit) =
    object: Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(observable: Observable?, i: Int) =
            callback(observable as T)
    }.also { addOnPropertyChangedCallback(it) }