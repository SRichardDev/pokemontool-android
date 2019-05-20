package io.stanc.pogotool.firebase.node

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.DatabaseKeys.ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.IS_EX
import io.stanc.pogotool.firebase.DatabaseKeys.LATITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.LONGITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.NAME
import io.stanc.pogotool.firebase.DatabaseKeys.RAID
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTER
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.MapGridProvider.Companion.GEO_HASH_AREA_PRECISION
import io.stanc.pogotool.RaidBossImageMapper

data class FirebaseArena(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String,
    val isEX: Boolean = false,
    // TODO: this should be a raidId, not a raidObject -> has to be changed on the firebase server structure: arenas/<geohash>/<arenaId>/data["raidId"] = raidId
    val raid: FirebaseRaid? = null): FirebaseNode {

    override fun databasePath() = "$ARENAS/${geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)}"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[NAME] = name
        data[IS_EX] = isEX
        data[LATITUDE] = geoHash.toLocation().latitude
        data[LONGITUDE] = geoHash.toLocation().longitude
        data[SUBMITTER] = submitter

        return data
    }

    data class IconConfig(val iconSize: Int, val innerIconSize: Int)

    fun icon(context: Context, iconConfig: IconConfig): Bitmap {

        val foregroundDrawable = RaidBossImageMapper.raidDrawable(context, this)
        Log.i(TAG, "Debug:: icon(arena: $this), foregroundDrawable: $foregroundDrawable")

        return if (isEX) {
            bitmap(context, iconConfig, R.drawable.icon_arena_ex_30dp, foregroundDrawable)
        } else {
            bitmap(context, iconConfig, R.drawable.icon_arena_30dp, foregroundDrawable)
        }
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseArena? {

//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val name = dataSnapshot.child(NAME).value as? String
            val isEX = (dataSnapshot.child(IS_EX).value as? Boolean) ?: kotlin.run {
                (dataSnapshot.child(IS_EX).value as? String)?.toBoolean()
            }
            val latitude = (dataSnapshot.child(LATITUDE).value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child(LATITUDE).value as? String)?.toDouble()
            }
            val longitude = (dataSnapshot.child(LONGITUDE).value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child(LONGITUDE).value as? String)?.toDouble()
            }
            val submitter = dataSnapshot.child(SUBMITTER).value as? String

//            Log.v(TAG, "id: $id, name: $name, isEX: $isEX, latitude: $latitude, longitude: $longitude, submitter: $submitter")

            if (id != null && name != null && isEX != null && latitude != null && longitude != null && submitter != null) {
                val geoHash = GeoHash(latitude, longitude)
                val raid = FirebaseRaid.new(id, geoHash, dataSnapshot.child(RAID))
                return FirebaseArena(id, name, geoHash, submitter, isEX, raid)
            }

            return null
        }

        fun baseIcon(context: Context, isEX: Boolean, iconConfig: IconConfig): Bitmap {
            return if (isEX) {
                bitmap(context, iconConfig, R.drawable.icon_arena_ex_30dp)
            } else {
                bitmap(context, iconConfig, R.drawable.icon_arena_30dp)
            }
        }

        private fun bitmap(context: Context, iconConfig: IconConfig, @DrawableRes backgroundDrawableRes: Int, foregroundDrawable: Drawable? = null): Bitmap {

            val bitmap = Bitmap.createBitmap(iconConfig.iconSize, iconConfig.iconSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val backgroundDrawable = context.getDrawable(backgroundDrawableRes)
            backgroundDrawable?.setBounds(0, 0, iconConfig.iconSize, iconConfig.iconSize)
            backgroundDrawable?.draw(canvas)

            foregroundDrawable?.let {

                val marginLeft = (iconConfig.iconSize - iconConfig.innerIconSize)/2
                val marginTop = (iconConfig.iconSize - iconConfig.innerIconSize)/2
                it.setBounds(marginLeft, marginTop, iconConfig.iconSize - marginLeft, iconConfig.iconSize - marginTop)
                it.draw(canvas)
            }

            return bitmap
        }

    }
}