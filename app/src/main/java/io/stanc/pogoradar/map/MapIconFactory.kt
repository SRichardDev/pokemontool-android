package io.stanc.pogoradar.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.IconFactory
import io.stanc.pogoradar.viewmodel.arena.RaidState
import io.stanc.pogoradar.viewmodel.arena.currentRaidState
import io.stanc.pogoradar.viewmodel.arena.raidTime
import io.stanc.pogoradar.viewmodel.pokestop.QuestViewModel

object MapIconFactory {
    private val TAG = javaClass.name

    /**
     * Arena Icon
     */

    fun arenaIcon(context: Context, arena: FirebaseArena, sizeMod: IconFactory.SizeMod, showFooterText: Boolean = false): Bitmap? {

        return backgroundDrawable(context, arena.isEX)?.let { backgroundDrawable ->

            val iconConfig = IconFactory.IconConfig(backgroundDrawable)

            FirebaseImageMapper.raidDrawable(context, arena)?.let { foregroundDrawable ->
                iconConfig.foregroundDrawable = foregroundDrawable
            }

            arena.raid?.let {
                if (currentRaidState(it) != RaidState.NONE) {
                    iconConfig.headerText = "[${raidTime(it)}]"
                }
            }

            if (showFooterText) {
                iconConfig.footerText = arena.name
            }
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

    fun pokestopIcon(context: Context, pokestop: FirebasePokestop, sizeMod: IconFactory.SizeMod, showFooterText: Boolean = false): Bitmap? {

        return ContextCompat.getDrawable(context, R.drawable.icon_pstop_30dp)?.let { backgroundDrawable ->

            val iconConfig = IconFactory.IconConfig(
                backgroundDrawable
            )

            if(QuestViewModel.new(pokestop, context).questExists.get() == true) {
                FirebaseImageMapper.questDrawable(context, pokestop)?.let { headerDrawable ->
                    iconConfig.headerDrawable = headerDrawable
                }
            }

            if (showFooterText) {
                iconConfig.footerText = pokestop.name
            }
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