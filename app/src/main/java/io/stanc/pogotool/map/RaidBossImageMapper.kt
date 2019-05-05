package io.stanc.pogotool.map

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.util.Log
import android.widget.ImageView
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebaseRaidboss
import io.stanc.pogotool.utils.WaitingSpinner
import java.io.IOException

object RaidBossImageMapper {

    private val TAG = javaClass.name
    var raidBosses = listOf<FirebaseRaidboss>()
        private set

    // TODO: load optional sprites from: https://github.com/PokeAPI/sprites
    fun loadRaidBosses(firebase: FirebaseDatabase) {

        WaitingSpinner.showProgress(R.string.spinner_title_raid_data)
        firebase.loadRaidBosses { firebaseRaidBosses ->

            firebaseRaidBosses?.let { raidBosses = it }
            WaitingSpinner.hideProgress()
        }
    }

    fun raidDrawable(context: Context, arena: FirebaseArena?): Drawable? {
        Log.d(TAG, "Debug:: raidDrawable for raid: ${arena?.raid}, eggIsAlreadyHatched: ${arena?.raid?.eggIsAlreadyHatched()}, raidBossId: ${arena?.raid?.raidBossId}, level: ${arena?.raid?.level}")

        arena?.raid?.let { raid ->

            raid.eggIsAlreadyHatched()?.let { eggIsHatched ->

                if (eggIsHatched) {

                    raid.raidBossId?.let { id ->
                        val imageName = raidBosses.first { it.id == id }.imageName
                        return raidBossDrawable(context, raidBossImageName = imageName)
                    }

                } else {

                    return when(raid.level) {
                        "1" -> context.getDrawable(R.drawable.icon_level_1_30dp)
                        "2" -> context.getDrawable(R.drawable.icon_level_2_30dp)
                        "3" -> context.getDrawable(R.drawable.icon_level_3_30dp)
                        "4" -> context.getDrawable(R.drawable.icon_level_4_30dp)
                        "5" -> context.getDrawable(R.drawable.icon_level_5_30dp)
                        else -> null
                    }
                }

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
}