package io.stanc.pogoradar.viewmodel.arena

import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RaidCreationViewModel: ViewModel(), Observable {
    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        Log.d(TAG, "Debug:: removeOnPropertyChangedCallback(callback: $callback)")
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        Log.d(TAG, "Debug:: addOnPropertyChangedCallback(callback: $callback)")
    }

    private val TAG = javaClass.name

    val isUserParticipate = MutableLiveData<Boolean>(false)
//    val test = MutableLiveData<Boolean>(false)
//
//    fun changeUser(participate: Boolean) {
//        Log.d(TAG, "Debug:: changeUser(participate: $participate)")
//        isUserParticipate.value = participate
//        test.value = participate
//    }

//    val isUserParticipate = ObservableField<Boolean>(false)
}