package io.stanc.pogotool.firebase.data

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_RAID_BOSSES

data class FirebaseRaidboss(
    override val id: String,
    val name: String,
    val level: String,
    val imageName: String): FirebaseNode {

    override fun databasePath(): String = DATABASE_RAID_BOSSES

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["name"] = name
        data["level"] = level
        data["imageName"] = imageName

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaidboss? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val name = dataSnapshot.child("name").value as? String
            val level = dataSnapshot.child("level").value as? String
            val imageName = dataSnapshot.child("imageName").value as? String

//            Log.v(TAG, "id: $id, name: $name, level: $level, imageName: $imageName")

            if (id != null && name != null && level != null && imageName != null) {
                return FirebaseRaidboss(id, name, level, imageName)
            }

            return null
        }
    }
}