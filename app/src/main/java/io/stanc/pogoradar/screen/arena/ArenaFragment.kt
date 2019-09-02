package io.stanc.pogoradar.screen.arena

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.chat.ChatMessage
import io.stanc.pogoradar.chat.ChatViewModel
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebaseChat
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.utils.ParcelableDataFragment
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.arena.ArenaViewModel
import io.stanc.pogoradar.viewmodel.arena.RaidStateViewModel
import io.stanc.pogoradar.viewmodel.arena.RaidViewModel


class ArenaFragment: ParcelableDataFragment<FirebaseArena>(), ChatViewModel.SendMessageDelegate {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    /**
     * Observer
     */

    private val arenaObserver = object: FirebaseNodeObserverManager.Observer<FirebaseArena> {

        override fun onItemChanged(item: FirebaseArena) {
            Log.v(TAG, "Debug:: arena.onItemChanged(item: $item)")
            dataObject = item
        }

        override fun onItemRemoved(itemId: String) {
            Log.v(TAG, "Debug:: arena.onItemRemoved(itemId: $itemId)")
            dataObject = null
        }
    }

    private val raidMeetupObserver = object: FirebaseNodeObserverManager.Observer<FirebaseRaidMeetup> {

        override fun onItemChanged(item: FirebaseRaidMeetup) {
            Log.v(TAG, "Debug:: raidMeetup.onItemChanged(item: $item)")
            activity?.let {
                val raidViewModel = ViewModelProviders.of(it).get(RaidViewModel::class.java)
                raidViewModel.updateData(item)
                ViewModelProviders.of(it).get(ChatViewModel::class.java).updateData(item, FirebaseUser.userData, raidViewModel.participants.value)
            }
        }

        override fun onItemRemoved(itemId: String) {
            Log.v(TAG, "Debug:: raidMeetup.onItemRemoved(itemId: $itemId)")
            if (dataObject?.raid?.raidMeetupId == itemId) {
                activity?.let {
                    ViewModelProviders.of(it).get(RaidViewModel::class.java).updateData(null)
                    ViewModelProviders.of(it).get(ChatViewModel::class.java).updateData(null, null, null)
                }
            }
        }
    }

    override fun onSendingMessage(senderId: String, text: String): String? {
        Log.i(TAG, "Debug:: onSendingMessage(senderId: $senderId, text: $text)")
        return dataObject?.raid?.raidMeetupId?.let { raidMeetupId ->

            val firebaseChatMessage = FirebaseChat.new(raidMeetupId, senderId, text)
            firebase.pushChatMessage(firebaseChatMessage)

        } ?: run {
            null
        }
    }

    /**
     * Lifecycle
     */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_arena, container, false)
        Log.i(TAG, "Debug:: onCreateView()")
        resetViewModels()

        activity?.let {
            ViewModelProviders.of(it).get(ChatViewModel::class.java).setDelegate(this)
        }

        dataObject?.let { arena ->
            firebase.addObserver(arenaObserver, arena)
            tryAddingRaidMeetupObserver(arena)
        }

        ShowFragmentManager.replaceFragment(ArenaInfoFragment(), childFragmentManager, R.id.arena_layout)

        return rootLayout
    }

    override fun onStart() {
        super.onStart()
        dataObject?.let { AppbarManager.setTitle(it.name) }
    }

    override fun onDestroyView() {
        removeObservers()
        dataObject = null
        resetViewModels()
        Log.i(TAG, "Debug:: onDestroyView()")
        super.onDestroyView()
    }

    override fun onDataChanged(dataObject: FirebaseArena?) {
        activity?.let {
            ViewModelProviders.of(it).get(ArenaViewModel::class.java).updateData(dataObject, it)
            ViewModelProviders.of(it).get(RaidViewModel::class.java).updateData(dataObject, it)
        }

        tryAddingRaidMeetupObserver(dataObject)
    }

    /**
     * Implementation
     */

    private fun resetViewModels() {
        activity?.let {
            ViewModelProviders.of(it).get(ArenaViewModel::class.java).reset()
            ViewModelProviders.of(it).get(RaidViewModel::class.java).reset()
            ViewModelProviders.of(it).get(RaidStateViewModel::class.java).reset()
            ViewModelProviders.of(it).get(ChatViewModel::class.java).reset()
        }
    }

    private fun tryAddingRaidMeetupObserver(arena: FirebaseArena?) {

        arena?.raid?.raidMeetupId?.let { raidMeetupId ->
            val raidMeetup = FirebaseRaidMeetup.new(raidMeetupId, DatabaseKeys.DEFAULT_MEETUP_TIME)
            firebase.addObserver(raidMeetupObserver, raidMeetup)
        }
    }

    private fun removeObservers() {

        dataObject?.let { arena ->

            firebase.removeObserver(arenaObserver, arena)

            arena.raid?.raidMeetupId?.let { raidMeetupId ->
                val raidMeetup = FirebaseRaidMeetup.new(raidMeetupId, DatabaseKeys.DEFAULT_MEETUP_TIME)
                firebase.removeObserver(raidMeetupObserver, raidMeetup)
            }
        }
    }
}