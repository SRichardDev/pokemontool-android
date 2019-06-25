package io.stanc.pogotool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.AppSettings

class ArenaViewModel(arena: FirebaseArena): ViewModel() {

    val isArenaVisibleOnMap = ObservableField<Boolean>()

    init {

        var visible = AppSettings.enableArenas.get() == true
        if (visible) {
            visible = !(!arena.isEX && AppSettings.justEXArenas.get() == true)
            if (visible) {
                val viewModel = RaidStateViewModel(arena.raid)
                visible = !(viewModel.isRaidAnnounced.get() == false && AppSettings.justRaidArenas.get() == true)
            }
        }
        isArenaVisibleOnMap.set(visible)
    }

}