package io.stanc.pogoradar.viewmodel.arena

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import io.stanc.pogoradar.firebase.node.FirebaseRaid
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.TimeCalculator

class RaidStateViewModel: ViewModel() {
    private val TAG = javaClass.name

    var raid: FirebaseRaid? = null

    val raidState = MutableLiveData<RaidState>(RaidState.NONE)
    val raidTime = MutableLiveData<String?>(null)
    val isRaidAnnounced = MutableLiveData<Boolean>(false)

    fun updateData(raid: FirebaseRaid) {
        Log.d(TAG, "Debug:: updateData(raid: $raid)")
        this.raid = raid

        val currentRaidState = currentRaidState(raid)
        raidState.value = currentRaidState
        raidTime.value = raidTime(raid)
        isRaidAnnounced.value = currentRaidState != RaidState.NONE
    }

    fun reset() {
        Log.w(TAG, "Debug:: reset()")
        raidState.value = RaidState.NONE
        raidTime.value = null
        isRaidAnnounced.value = false
    }
}