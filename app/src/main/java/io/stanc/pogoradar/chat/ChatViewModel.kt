package io.stanc.pogoradar.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.firebase.node.FirebaseChat
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import java.lang.ref.WeakReference

class ChatViewModel: ViewModel() {

    private val TAG = javaClass.name

    interface ReceiveMessageDelegate {
        fun onReceivedMessage(message: ChatMessage)
        fun onUpdateMessageList(messages: List<ChatMessage>)
    }

    interface SendMessageDelegate {
        fun onSendingMessage(senderId: String, text: String): String?
    }

    private var receiveMessageDelegate: WeakReference<ReceiveMessageDelegate>? = null
    private var sendMessageDelegate: WeakReference<SendMessageDelegate>? = null

    var userId: String? = null
    var userName: String? = null
    var chatMessages: MutableList<ChatMessage> = mutableListOf()
    private var chatParticipants: List<FirebasePublicUser>? = null

    fun updateUser(user: FirebaseUserNode?) {
        Log.d(TAG, "Debug:: updateUser(user: $user)")
        user?.let {

            userId = user.id
            userName = user.name

        } ?: run {

            userId = null
            userName = null
        }
    }

    fun updateChatParticipants(chatParticipants: List<FirebasePublicUser>?) {
        Log.d(TAG, "Debug:: updateChatParticipants(chats: $chatParticipants)")
        this.chatParticipants = chatParticipants
    }

    fun updateChatMessages(chats: List<FirebaseChat>?) {
        Log.d(TAG, "Debug:: updateChatMessages(chats: $chats)")

        chats?.let {

            chatMessages = chats.map { chat ->
                chatParticipants?.firstOrNull { it.id == chat.senderId}?.let { publicUser ->
                    ChatMessage.new(chat, publicUser)
                }
            }.filterNotNull().toMutableList()

        } ?: run {

            chatMessages = mutableListOf()
        }

        receiveMessageDelegate?.get()?.onUpdateMessageList(chatMessages)
    }

    fun messageReceived(message: FirebaseChat) {
        chatParticipants?.firstOrNull { it.id == message.senderId}?.let { publicUser ->

            val chatMessage = ChatMessage.new(message, publicUser)
            chatMessages.add(chatMessage)
            receiveMessageDelegate?.get()?.onReceivedMessage(chatMessage)
        }
    }

    fun messageRemoved(message: FirebaseChat) {
        // TODO: ...
    }

    fun reset() {
        Log.d(TAG, "Debug:: reset()")

        userId = null
        userName = null
        chatMessages = mutableListOf()
        chatParticipants = null

        receiveMessageDelegate = null
        sendMessageDelegate = null
    }

    fun newMessage(senderId: String, text: String): String? {
        return sendMessageDelegate?.get()?.onSendingMessage(senderId, text)
    }

    fun setDelegate(delegate: ReceiveMessageDelegate) {
        receiveMessageDelegate = WeakReference(delegate)
    }

    fun setDelegate(delegate: SendMessageDelegate) {
        sendMessageDelegate = WeakReference(delegate)
    }
}