package io.stanc.pogoradar.viewmodel.arena

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RaidCreationViewModel: ViewModel() {

    private val TAG = javaClass.name

    val isUserParticipate = MutableLiveData<Boolean>(false)
}