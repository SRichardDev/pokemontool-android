package io.stanc.pogoradar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.AppSettings
import io.stanc.pogoradar.firebase.node.FirebasePokestop

class ArenaViewModel: ViewModel() {

    var arena: FirebaseArena? = null
    val isArenaVisibleOnMap = ObservableField<Boolean>()

    fun updateData(arena: FirebaseArena) {
        this.arena = arena

        var visible = AppSettings.enableArenas.get() == true
        if (visible) {
            visible = !(!arena.isEX && AppSettings.justEXArenas.get() == true)
            if (visible) {
                val viewModel = RaidStateViewModel.new(arena.raid)
                visible = !(viewModel.isRaidAnnounced.get() == false && AppSettings.justRaidArenas.get() == true)
            }
        }
        isArenaVisibleOnMap.set(visible)
    }

    companion object {

        fun new(arena: FirebaseArena): ArenaViewModel {
            val viewModel = ArenaViewModel()
            viewModel.updateData(arena)
            return viewModel
        }
    }

}