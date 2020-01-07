package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.CHATS
import kotlinx.android.parcel.Parcelize

@Parcelize
class FirebaseChat private constructor(
    override val id: String,
    val chat: MutableList<FirebaseChatMessage>): FirebaseListNode, Parcelable {

    override fun databasePath(): String = CHATS

    override fun list(): List<FirebaseNode> = chat.toList()

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseChat? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key ?: return null

            val chat = mutableListOf<FirebaseChatMessage>()
            for (chatSnapshot in dataSnapshot.children) {
                val databasePath = "$CHATS/$id"
                FirebaseChatMessage.new(databasePath, chatSnapshot)?.let { chat.add(it) }
            }

            Log.v(TAG, "id: $id, chat: $chat")

            return FirebaseChat(id, chat.toMutableList())
        }

        fun new(): FirebaseChat {
            return FirebaseChat("", mutableListOf())
        }
    }
}