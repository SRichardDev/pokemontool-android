package io.stanc.pogoradar.viewmodel.arena

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.map.MapIconFactory
import io.stanc.pogoradar.utils.IconFactory
import io.stanc.pogoradar.viewmodel.MapItemInfoViewModel

class ArenaViewModel: ViewModel() {

    var arena: FirebaseArena? = null
    val mapItemInfoViewModel = MapItemInfoViewModel()
    
    val isArenaVisibleOnMap = MutableLiveData<Boolean>()
    val arenaImage = MutableLiveData<Drawable?>()

    fun updateData(arena: FirebaseArena?, context: Context) {
        this.arena = arena

        arena?.let {

            isArenaVisibleOnMap.value = isArenaVisibleOnMap(it)
            arenaImage.value = imageDrawable(arena, context)
            mapItemInfoViewModel.updateData(arena.submitter, arena.geoHash)

        } ?: run {
            reset()
        }
    }

    fun reset() {
        isArenaVisibleOnMap.value = false
        arenaImage.value = null
    }

    private fun imageDrawable(arena: FirebaseArena, context: Context): Drawable? {
        val bitmap = MapIconFactory.arenaIcon(context, arena, IconFactory.SizeMod.LARGE)
        return BitmapDrawable(context.resources, bitmap)
    }
}