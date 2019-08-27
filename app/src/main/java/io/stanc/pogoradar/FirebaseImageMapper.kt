package io.stanc.pogoradar

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import io.stanc.pogoradar.firebase.FirebaseDefinitions.raidBosses
import io.stanc.pogoradar.firebase.FirebaseDefinitions.quests
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.firebase.node.Team
import io.stanc.pogoradar.viewmodel.arena.RaidState
import io.stanc.pogoradar.viewmodel.arena.currentRaidState
import java.io.IOException

object FirebaseImageMapper {

    private val TAG = javaClass.name

    const val ASSETS_DIR_RAIDBOSSES = "raidbosses"
    const val ASSETS_DIR_REWARDS = "rewards"

    val TEAM_COLOR = mapOf(Team.MYSTIC to R.color.teamMystic, Team.VALOR to R.color.teamValor, Team.INSTINCT to R.color.teamInstinct)

    fun raidDrawable(context: Context, arena: FirebaseArena?): Drawable? {

        arena?.raid?.let { raid ->

            when(currentRaidState(arena.raid)) {

                RaidState.RAID_RUNNING -> {

                    return arena.raid.raidBossId?.let { raidBossId ->

                        raidBossDrawable(context, raidBossId)

                    } ?: run {
                        raidBossPlaceholerDrawable(context, raid.level)
                    }
                }

                RaidState.EGG_HATCHES -> {
                    return eggDrawable(context, raid.level)
                }

                RaidState.NONE -> return null
            }
        }

        return null
    }

    private fun eggDrawable(context: Context, raidLevel: Int): Drawable? {
        return when(raidLevel) {
            1 -> context.getDrawable(R.drawable.icon_level_1_30dp)
            2 -> context.getDrawable(R.drawable.icon_level_2_30dp)
            3 -> context.getDrawable(R.drawable.icon_level_3_30dp)
            4 -> context.getDrawable(R.drawable.icon_level_4_30dp)
            5 -> context.getDrawable(R.drawable.icon_level_5_30dp)
            else -> null
        }
    }

    private fun raidBossDrawable(context: Context, raidBossId: String): Drawable? {
        raidBosses.find { it.id == raidBossId }?.imageName?.let { imageName ->
            return assetDrawable(
                context,
                ASSETS_DIR_RAIDBOSSES,
                assetImageName = imageName
            )
        } ?: run {
            Log.e(TAG, "could not determine raidBoss drawable for raidBossId: $raidBossId")
            return null
        }
    }

    private fun raidBossPlaceholerDrawable(context: Context, raidLevel: Int): Drawable? {
        // TODO: need alpha value, not white
        return when(raidLevel) {
            1 -> context.getDrawable(R.drawable.icon_level_1_hatched)
            2 -> context.getDrawable(R.drawable.icon_level_2_hatched)
            3 -> context.getDrawable(R.drawable.icon_level_3_hatched)
            4 -> context.getDrawable(R.drawable.icon_level_4_hatched)
            5 -> context.getDrawable(R.drawable.icon_level_5_hatched)
            else -> null
        }
    }

    fun assetDrawable(context: Context, assetDir: String, assetImageName: String): Drawable? {

        return try {
            val inputStream = context.assets.open("$assetDir/$assetImageName.png")
            Drawable.createFromStream(inputStream, null)

        } catch (ex: IOException) {
            Log.e(TAG, ex.toString())
            null
        }
    }

    fun questDrawable(context: Context, pokestop: FirebasePokestop): Drawable? {

        return quests.find { it.id == pokestop.quest?.definitionId }?.let { questDefinition ->
            questDrawable(context, questDefinition.imageName)
        } ?: run {
            null
        }
    }

    fun questDrawable(context: Context, imageName: String): Drawable? {

        return when (imageName) {
            "candy" -> assetDrawable(context, ASSETS_DIR_REWARDS, assetImageName = imageName)
            "stardust" -> assetDrawable(context, ASSETS_DIR_REWARDS, assetImageName = imageName)
            else -> assetDrawable(context, ASSETS_DIR_RAIDBOSSES, assetImageName = imageName)
        }
    }
}