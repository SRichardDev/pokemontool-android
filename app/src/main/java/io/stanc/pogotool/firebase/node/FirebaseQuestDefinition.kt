package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST
import io.stanc.pogotool.firebase.DatabaseKeys.QUESTS
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST_IMAGE_NAME
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST_REWARD

class FirebaseQuestDefinition(
    override val id: String,
    val quest: String,
    val imageName: String,
    val reward: String): FirebaseNode {

    override fun databasePath(): String = QUESTS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[QUEST] = quest
        data[QUEST_IMAGE_NAME] = imageName
        data[QUEST_REWARD] = reward

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseQuestDefinition? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val quest = dataSnapshot.child(QUEST).value as? String
            val imageName = dataSnapshot.child(QUEST_IMAGE_NAME).value as? String
            val reward = dataSnapshot.child(QUEST_REWARD).value as? String

            Log.v(TAG, "id: $id, quest: $quest, imageName: $imageName, reward: $reward")

            if (id != null && quest != null && imageName != null && reward != null) {
                return FirebaseQuestDefinition(id, quest, imageName, reward)
            }

            return null
        }
    }
}