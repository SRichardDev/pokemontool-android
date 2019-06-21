package io.stanc.pogotool.firebase.node

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.NAME
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSSES
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSS_IMAGE_NAME
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSS_LEVEL

data class FirebaseRaidbossDefinition(
    override val id: String,
    val name: String,
    val level: String,
    val imageName: String): FirebaseNode {

    override fun databasePath(): String = RAID_BOSSES

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[NAME] = name
        data[RAID_BOSS_LEVEL] = level
        data[RAID_BOSS_IMAGE_NAME] = imageName

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaidbossDefinition? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val name = dataSnapshot.child(NAME).value as? String
            val level = dataSnapshot.child(RAID_BOSS_LEVEL).value as? String
            val imageName = dataSnapshot.child(RAID_BOSS_IMAGE_NAME).value as? String

//            Log.v(TAG, "id: $id, name: $name, level: $level, imageName: $imageName")

            if (id != null && name != null && level != null && imageName != null) {
                return FirebaseRaidbossDefinition(id, name, level, imageName)
            }

            return null
        }
    }
}