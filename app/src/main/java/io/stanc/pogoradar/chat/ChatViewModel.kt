package io.stanc.pogoradar.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup

class ChatViewModel: ViewModel() {

    private val TAG = javaClass.name

    var raidMeetup: FirebaseRaidMeetup? = null
    var userId: String? = null
    var userName: String? = null
    var messages: List<ChatMessage> = mutableListOf()

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private val raidMeetupObserver = object: FirebaseNodeObserverManager.Observer<FirebaseRaidMeetup> {

        override fun onItemChanged(item: FirebaseRaidMeetup) {
            Log.d(TAG, "Debug:: onItemChanged(item: $item)")
            messages = mutableListOf()
            item.chats.forEach { messages.add(ChatMessage.new()) }
        }

        override fun onItemRemoved(itemId: String) {
            Log.w(TAG, "Debug:: onItemRemoved(itemId: $itemId)")
        }
    }

    fun updateData(raidMeetup: FirebaseRaidMeetup?) {

        raidMeetup?.let {
            firebase.addObserver(raidMeetupObserver, it)

        } ?: run {
            reset()
        }

        this.raidMeetup = raidMeetup
    }

    fun reset() {
        raidMeetup?.let { firebase.removeObserver(raidMeetupObserver, it) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}