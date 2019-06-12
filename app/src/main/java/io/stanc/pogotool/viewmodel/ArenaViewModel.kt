package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.map.MapSettings

class ArenaViewModel(arena: FirebaseArena): ViewModel() {

    val isArenaVisibleOnMap = ObservableField<Boolean>()

    init {

        var visible = MapSettings.enableArenas.get() == true
        if (visible) {
            visible = !(!arena.isEX && MapSettings.justEXArenas.get() == true)
            if (visible) {
                val viewModel = RaidStateViewModel(arena.raid)
                visible = !(viewModel.isRaidAnnounced.get() == false && MapSettings.justRaidArenas.get() == true)
            }
        }
        isArenaVisibleOnMap.set(visible)
    }

}