package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.AppSettings

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