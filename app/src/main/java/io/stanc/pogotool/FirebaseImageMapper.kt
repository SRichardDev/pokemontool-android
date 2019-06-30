package io.stanc.pogotool

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import io.stanc.pogotool.firebase.FirebaseDefinitions.raidBosses
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.viewmodel.RaidStateViewModel
import io.stanc.pogotool.viewmodel.RaidStateViewModel.RaidState
import java.io.IOException

object FirebaseImageMapper {

    private val TAG = javaClass.name

    fun raidDrawable(context: Context, arena: FirebaseArena?): Drawable? {

        arena?.raid?.let { raid ->
//            Log.v(TAG, "Debug:: raidDrawable for raid: $raid, currentRaidState: ${RaidStateViewModel(raid).currentRaidState().name}, raidBossId: ${raid.raidBossId}, level: ${raid.level}")

            when(RaidStateViewModel(raid).currentRaidState()) {

                RaidState.RAID_RUNNING -> {
                    raid.raidBossId?.let { id ->
                        val imageName = raidBosses.first { it.id == id }.imageName
                        return raidBossDrawable(
                            context,
                            raidBossImageName = imageName
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

    fun raidBossDrawable(context: Context, raidBossImageName: String): Drawable? {

        return try {
            val inputStream = context.assets.open("raidbosses/$raidBossImageName.png")
            Drawable.createFromStream(inputStream, null)

        } catch (ex: IOException) {
            Log.e(TAG, ex.toString())
            null
        }
    }

    fun questDrawable(context: Context, imageName: String): Drawable? {

        return when (imageName) {
            "candy" -> context.getDrawable(R.drawable.icon_candy_96dp)
            "stardust" -> context.getDrawable(R.drawable.icon_candy_96dp)
            else -> raidBossDrawable(context,raidBossImageName = imageName)
        }
    }
}