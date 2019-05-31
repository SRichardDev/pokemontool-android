package io.stanc.pogotool.firebase.node

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST
import io.stanc.pogotool.firebase.DatabaseKeys.QUESTS
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST_IMAGE_NAME
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST_REWARD

class FirebaseQuestDefinition(
    override val id: String,
    val questDescription: String,
    val imageName: String,
    val reward: String): FirebaseNode {

    override fun databasePath(): String = QUESTS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[QUEST] = questDescription
        data[QUEST_IMAGE_NAME] = imageName
        data[QUEST_REWARD] = reward

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseQuestDefinition? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val questDescription = dataSnapshot.child(QUEST).value as? String
            val imageName = dataSnapshot.child(QUEST_IMAGE_NAME).value as? String
            val reward = dataSnapshot.child(QUEST_REWARD).value as? String

//            Log.v(TAG, "id: $id, questDescription: $questDescription, imageName: $imageName, reward: $reward")
            if (id != null && questDescription != null && imageName != null && reward != null) {
                return FirebaseQuestDefinition(id, questDescription, imageName, reward)
            }

            return null
        }
    }
}