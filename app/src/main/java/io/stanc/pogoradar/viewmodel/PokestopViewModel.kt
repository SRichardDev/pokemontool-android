package io.stanc.pogoradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.AppSettings

class PokestopViewModel(pokestop: FirebasePokestop): ViewModel() {

    val isPokestopVisibleOnMap = ObservableField<Boolean>()

    init {

        var visible = AppSettings.enablePokestops.get() == true
        if (visible) {
            val viewModel = QuestViewModel(pokestop)
            visible = !(viewModel.questExists.get() == false && AppSettings.justQuestPokestops.get() == true)
        }
        isPokestopVisibleOnMap.set(visible)
    }

}