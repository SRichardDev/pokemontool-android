package io.stanc.pogoradar.viewmodel

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.AppSettings
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.map.MapIconFactory
import io.stanc.pogoradar.utils.IconFactory

class ArenaViewModel: ViewModel() {

    var arena: FirebaseArena? = null
    val isArenaVisibleOnMap = ObservableField<Boolean>()
    val arenaImage = ObservableField<Drawable?>()
    val mapItemInfoViewModel = MapItemInfoViewModel()

    fun updateData(arena: FirebaseArena?, context: Context) {
        this.arena = arena

        arena?.let {

            var visible = AppSettings.enableArenas.get() == true
            if (visible) {
                visible = !(!arena.isEX && AppSettings.justEXArenas.get() == true)
                if (visible) {
                    val viewModel = RaidStateViewModel.new(arena.raid)
                    visible = !(viewModel.isRaidAnnounced.get() == false && AppSettings.justRaidArenas.get() == true)
                }
            }
            isArenaVisibleOnMap.set(visible)
            arenaImage.set(imageDrawable(arena, context))
            mapItemInfoViewModel.updateData(arena.submitter, arena.geoHash)

        } ?: run {
            reset()
        }
    }

    fun reset() {
        isArenaVisibleOnMap.set(false)
        arenaImage.set(null)
    }

    private fun imageDrawable(arena: FirebaseArena, context: Context): Drawable? {
        val bitmap = MapIconFactory.arenaIcon(context, arena, IconFactory.SizeMod.LARGE)
        return BitmapDrawable(context.resources, bitmap)
    }

    companion object {

        fun new(arena: FirebaseArena, context: Context): ArenaViewModel {
            val viewModel = ArenaViewModel()
            viewModel.updateData(arena, context)
            return viewModel
        }
    }

}