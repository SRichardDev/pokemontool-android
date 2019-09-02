package io.stanc.pogoradar.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.firebase.node.FirebaseChat
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.viewmodel.arena.RaidViewModel
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

    var raidMeetup: FirebaseRaidMeetup? = null
    var userId: String? = null
    var userName: String? = null
    var messages: List<ChatMessage> = mutableListOf()

    fun updateData(raidMeetup: FirebaseRaidMeetup?, user: FirebaseUserNode?, chatParticipants: List<FirebasePublicUser>?) {
        Log.d(TAG, "Debug:: updateData(raidMeetup: $raidMeetup, user: $user, chatParticipants: $chatParticipants)")
        raidMeetup?.let {

            userId = user?.id
            userName = user?.name
            messages = raidMeetup.chats.map { chat ->
                chatParticipants?.firstOrNull { it.id == chat.senderId}?.let { publicUser ->
                    ChatMessage.new(chat, publicUser)
                }
            }.filterNotNull()

            receiveMessageDelegate?.get()?.onUpdateMessageList(messages)

        } ?: run {
            reset()
        }

        this.raidMeetup = raidMeetup
    }

    fun reset() {
        Log.d(TAG, "Debug:: reset()")
        raidMeetup = null

        userId = null
        userName = null
        messages = mutableListOf()

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