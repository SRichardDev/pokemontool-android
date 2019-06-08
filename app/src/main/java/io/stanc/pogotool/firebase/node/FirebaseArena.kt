package io.stanc.pogotool.firebase.node

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.DatabaseKeys.ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.IS_EX
import io.stanc.pogotool.firebase.DatabaseKeys.LATITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.LONGITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.NAME
import io.stanc.pogotool.firebase.DatabaseKeys.RAID
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTER
import io.stanc.pogotool.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.IconFactory
import io.stanc.pogotool.viewmodel.RaidStateViewModel


data class FirebaseArena private constructor(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String,
    val isEX: Boolean = false,
    val raid: FirebaseRaid? = null): FirebaseNode {

    override fun databasePath() = "$ARENAS/${firebaseGeoHash(geoHash)}"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[NAME] = name
        data[IS_EX] = isEX
        data[LATITUDE] = geoHash.toLocation().latitude
        data[LONGITUDE] = geoHash.toLocation().longitude
        data[SUBMITTER] = submitter

        return data
    }

    fun icon(context: Context, iconSizeConfig: IconFactory.IconSizeConfig): Bitmap? {

        return backgroundDrawable(context, isEX)?.let { backgroundDrawable ->

            val iconConfig = IconFactory.IconConfig(
                backgroundConfig = IconFactory.DrawableConfig(backgroundDrawable, iconSizeConfig.backgroundSize)
            )

            FirebaseImageMapper.raidDrawable(context, this)?.let { foregroundDrawable ->
                iconConfig.foregroundConfig = IconFactory.DrawableConfig(foregroundDrawable, iconSizeConfig.foregroundSize)
            }

            // TODO: debug:
            raid?.let {
                val viewModel = RaidStateViewModel(it)
                if (viewModel.isRaidAnnounced.get() == true) {
                    iconConfig.headerText = "[${viewModel.raidTime.get()}]"
                }
            }

            return IconFactory.bitmap(context, iconConfig)

        } ?: kotlin.run {
            null
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

        fun new(name: String, geoHash: GeoHash, userId: String, isEX: Boolean): FirebaseArena {
            return FirebaseArena("", name, geoHash, userId, isEX)
        }

        fun baseIcon(context: Context, isEX: Boolean, iconSize: IconFactory.IconSizeConfig): Bitmap? {

            backgroundDrawable(context, isEX)?.let { drawable ->

                val iconConfig = IconFactory.IconConfig (
                    backgroundConfig = IconFactory.DrawableConfig(drawable, iconSize.backgroundSize)
                )
                return IconFactory.bitmap(context, iconConfig)

            } ?: kotlin.run {
                Log.e(TAG, "could not create baseIcon, because backgroundDrawable is null.")
                return null
            }
        }

        private fun backgroundDrawable(context: Context, isEX: Boolean): Drawable? {
            return if (isEX) {
                context.getDrawable(R.drawable.icon_arena_ex_30dp)
            } else {
                context.getDrawable(R.drawable.icon_arena_30dp)
            }
        }
    }
}