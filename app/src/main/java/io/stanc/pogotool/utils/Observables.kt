package io.stanc.pogotool.utils

import android.databinding.Observable
import android.databinding.ObservableField

object Observables {

    inline fun <T> dependantObservableField(vararg dependencies: Observable, defaultValue: T? = null, crossinline mapper: () -> T?) =
        object : ObservableField<T>(*dependencies) {
            override fun get(): T? {
                return mapper()
            }
        }.apply { set(defaultValue) }
}