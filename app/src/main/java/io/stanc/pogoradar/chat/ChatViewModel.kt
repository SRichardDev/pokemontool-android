package io.stanc.pogoradar.chat

import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.node.FirebaseChat
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import java.lang.ref.WeakReference

class ChatViewModel: ViewModel() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    interface ReceiveMessageDelegate {
        fun onReceivedMessage(message: ChatMessage)
    }

    interface SendMessageDelegate {
        fun onSendingMessage(senderId: String, text: String): String?
    }

    private var receiveMessageDelegate: WeakReference<ReceiveMessageDelegate>? = null
    private var sendMessageDelegate: WeakReference<SendMessageDelegate>? = null

    var userId: String? = null
    var userName: String? = null
    var chatMessages: MutableList<ChatMessage> = mutableListOf()

    fun setUser(user: FirebaseUserNode) {
        userId = user.id
        userName = user.name
    }

    fun receiveMessage(message: FirebaseChat) {
        onMessageReceived(message)
    }

    fun removeMessage(message: FirebaseChat) {
        // TODO: ...
    }

    fun writeNewMessage(senderId: String, text: String): String? {
        return sendMessageDelegate?.get()?.onSendingMessage(senderId, text)
    }

    fun setDelegate(delegate: ReceiveMessageDelegate) {
        receiveMessageDelegate = WeakReference(delegate)
    }

    fun setDelegate(delegate: SendMessageDelegate) {
        sendMessageDelegate = WeakReference(delegate)
    }

    fun reset() {
        userId = null
        userName = null
        chatMessages = mutableListOf()

        receiveMessageDelegate = null
        sendMessageDelegate = null
    }

    private fun onMessageReceived(message: FirebaseChat) {

        firebase.loadPublicUser(message.senderId, onCompletionCallback = { publicUser ->

            val chatMessage = ChatMessage.new(message, publicUser)
            if (!chatMessages.any { it.id == chatMessage.id }) {
                chatMessages.add(chatMessage)
                receiveMessageDelegate?.get()?.onReceivedMessage(chatMessage)
            }
        })
    }
}