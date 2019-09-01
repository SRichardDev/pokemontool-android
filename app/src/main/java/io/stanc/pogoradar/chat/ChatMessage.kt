package io.stanc.pogoradar.chat

import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import io.stanc.pogoradar.firebase.node.FirebaseChat
import io.stanc.pogoradar.utils.TimeCalculator
import java.util.*

class ChatMessage private constructor(
    private val id: String,
    private val date: Date,
    private val user: ChatUser,
    private val text: String
): IMessage {

    override fun getId(): String = id
    override fun getCreatedAt(): Date = date
    override fun getUser(): IUser = user
    override fun getText(): String = text

    companion object {

        fun new(firebaseChatMessage: FirebaseChat): ChatMessage {
            val date = Date(firebaseChatMessage.timestamp as Long)
            val user = ChatUser.new()
            val message = ChatMessage(firebaseChatMessage.id, date, )
        }

        fun new(messageId: String, userId: String, userName: String, text: String): ChatMessage {
            val date = TimeCalculator.currentDate()
            val user = ChatUser.new(userId, userName)
            return ChatMessage(messageId, date, user, text)
        }
    }
}