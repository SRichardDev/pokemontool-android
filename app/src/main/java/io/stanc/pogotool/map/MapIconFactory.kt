package io.stanc.pogotool.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.utils.IconFactory
import io.stanc.pogotool.viewmodel.QuestViewModel
import io.stanc.pogotool.viewmodel.RaidStateViewModel

object MapIconFactory {
    private val TAG = javaClass.name

    /**
     * Arena Icon
     */

    fun arenaIcon(context: Context, arena: FirebaseArena, sizeMod: IconFactory.SizeMod): Bitmap? {

        return backgroundDrawable(context, arena.isEX)?.let { backgroundDrawable ->

            val iconConfig = IconFactory.IconConfig(
                backgroundDrawable
            )

            FirebaseImageMapper.raidDrawable(context, arena)?.let { foregroundDrawable ->
                iconConfig.foregroundDrawable = foregroundDrawable
            }

            arena.raid?.let {
                val viewModel = RaidStateViewModel(it)
                if (viewModel.isRaidAnnounced.get() == true) {
                    iconConfig.headerText = "[${viewModel.raidTime.get()}]"
                }
            }

            iconConfig.footerText = arena.name
            iconConfig.sizeMod = sizeMod

            IconFactory.bitmap(context, iconConfig)

        } ?: run {
            null
        }
    }

    fun arenaIconBase(context: Context, isEX: Boolean, sizeMod: IconFactory.SizeMod): Bitmap? {

        return backgroundDrawable(context, isEX)?.let { drawable ->

            val iconConfig = IconFactory.IconConfig(drawable, null, null, sizeMod)
            IconFactory.bitmap(context, iconConfig)

        } ?: run {
            Log.e(TAG, "could not create arenaIconBase, because backgroundDrawable is null.")
            null
        }
    }

    private fun backgroundDrawable(context: Context, isEX: Boolean): Drawable? {
        return if (isEX) {
            ContextCompat.getDrawable(context, R.drawable.icon_arena_ex_30dp)
        } else {
            ContextCompat.getDrawable(context, R.drawable.icon_arena_30dp)
        }
    }

    /**
     * Pokestop Icon
     */

    fun pokestopIcon(context: Context, pokestop: FirebasePokestop, sizeMod: IconFactory.SizeMod): Bitmap? {

        return ContextCompat.getDrawable(context, R.drawable.icon_pstop_30dp)?.let { backgroundDrawable ->

            val iconConfig = IconFactory.IconConfig(
                backgroundDrawable
            )

            if(QuestViewModel(pokestop).questExists.get() == true) {
                FirebaseImageMapper.questDrawable(context, pokestop)?.let { headerDrawable ->
                    iconConfig.headerDrawable = headerDrawable
                }
            }

            iconConfig.footerText = pokestop.name
            iconConfig.sizeMod = sizeMod

            IconFactory.bitmap(context, iconConfig)

        } ?: run {
            null
        }
    }

    fun pokestopIconBase(context: Context, sizeMod: IconFactory.SizeMod): Bitmap? {
        return IconFactory.bitmap(context, R.drawable.icon_pstop_30dp, sizeMod)
    }
}