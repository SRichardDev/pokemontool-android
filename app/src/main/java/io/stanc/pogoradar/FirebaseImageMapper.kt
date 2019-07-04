package io.stanc.pogoradar

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import io.stanc.pogoradar.firebase.FirebaseDefinitions
import io.stanc.pogoradar.firebase.FirebaseDefinitions.raidBosses
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.firebase.node.Team
import io.stanc.pogoradar.viewmodel.RaidStateViewModel
import io.stanc.pogoradar.viewmodel.RaidStateViewModel.RaidState
import java.io.IOException

object FirebaseImageMapper {

    private val TAG = javaClass.name

    const val ASSETS_DIR_RAIDBOSSES = "raidbosses"
    const val ASSETS_DIR_REWARDS = "rewards"

    val TEAM_COLOR = mapOf(Team.MYSTIC to R.color.teamMystic, Team.VALOR to R.color.teamValor, Team.INSTINCT to R.color.teamInstinct)

    fun raidDrawable(context: Context, arena: FirebaseArena?): Drawable? {

        arena?.raid?.let { raid ->
//            Log.v(TAG, "Debug:: raidDrawable for raid: $raid, currentRaidState: ${RaidStateViewModel(raid).currentRaidState().name}, raidBossId: ${raid.raidBossId}, level: ${raid.level}")

            when(RaidStateViewModel(raid).currentRaidState()) {

                RaidState.RAID_RUNNING -> {
                    raid.raidBossId?.let { id ->
                        val imageName = raidBosses.first { it.id == id }.imageName
                        return assetDrawable(
                            context,
                            ASSETS_DIR_RAIDBOSSES,
                            assetImageName = imageName
                        )
                    }
                }

                RaidState.EGG_HATCHES -> {
                    return when(raid.level) {
                        1 -> context.getDrawable(R.drawable.icon_level_1_30dp)
                        2 -> context.getDrawable(R.drawable.icon_level_2_30dp)
                        3 -> context.getDrawable(R.drawable.icon_level_3_30dp)
                        4 -> context.getDrawable(R.drawable.icon_level_4_30dp)
                        5 -> context.getDrawable(R.drawable.icon_level_5_30dp)
                        else -> null
                    }
                }

                RaidState.NONE -> return null
            }
        }

        return null
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

        return FirebaseDefinitions.quests.find { it.id == pokestop.quest?.definitionId }?.let { questDefinition ->
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