package io.stanc.pogoradar.viewmodel

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import java.lang.ref.WeakReference

object ViewModelFactory {
    private val TAG = javaClass.name

    private var boundedViewModels: HashMap<Class<out ViewModel>, WeakReference<Fragment>> = HashMap()

    fun boundNewViewModel(fragment: Fragment, viewModelClass: Class<out ViewModel>) {
        boundedViewModels[viewModelClass] = WeakReference(fragment)
        ViewModelProviders.of(fragment).get(viewModelClass)
        Log.d(TAG, "Debug:: boundNewViewModel(fragment: $fragment, viewModelClass: ${viewModelClass.name}) boundedViewModels: $boundedViewModels")
    }

    fun <T: ViewModel>getViewModel(viewModelClass: Class<T>): T? {
        return boundedViewModels[viewModelClass]?.get()?.let {
            ViewModelProviders.of(it).get(viewModelClass)
        } ?: run { null }
    }
}