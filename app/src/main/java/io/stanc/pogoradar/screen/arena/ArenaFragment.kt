package io.stanc.pogoradar.screen.arena

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.chat.ChatViewModel
import io.stanc.pogoradar.firebase.*
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebaseChat
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.ParcelableDataFragment
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.arena.ArenaViewModel
import io.stanc.pogoradar.viewmodel.arena.RaidViewModel
import android.content.Intent
import android.util.Log
import io.stanc.pogoradar.App
import io.stanc.pogoradar.appbar.Toolbar
import io.stanc.pogoradar.firebase.DatabaseKeys.DEFAULT_MEETUP_TIME
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP_NONE
import io.stanc.pogoradar.firebase.node.FirebaseRaid
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.TimeCalculator


class ArenaFragment: ParcelableDataFragment<FirebaseArena>(), ChatViewModel.SendMessageDelegate {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private var arenaViewModel: ArenaViewModel? = null
    private var raidViewModel: RaidViewModel? = null
    private var chatViewModel: ChatViewModel? = null

    private val arenaObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebaseArena.new(dataSnapshot)
    })

    /**
     * FirebaseNodeObserver
     */

    private val arenaObserver = object: FirebaseNodeObserver<FirebaseArena> {

        override fun onItemChanged(item: FirebaseArena) {
            dataObject = item
        }

        override fun onItemRemoved(itemId: String) {
            dataObject = null
        }
    }

    private val raidMeetupObserver = object: FirebaseNodeObserver<FirebaseRaidMeetup> {

        override fun onItemChanged(item: FirebaseRaidMeetup) {
            updateRaidMeetup(item)
        }

        override fun onItemRemoved(itemId: String) {}
    }

    private val chatObserver = object: FirebaseServer.OnChildDidChangeCallback {

        override fun childAdded(dataSnapshot: DataSnapshot) {
            dataObject?.raid?.raidMeetupId?.let { raidMeetupId ->
                FirebaseChat.new(raidMeetupId, dataSnapshot)?.let { chat ->
                    chatViewModel?.receiveMessage(chat)
                }
            }
        }

        override fun childChanged(dataSnapshot: DataSnapshot) {
            // TODO ...
        }

        override fun childRemoved(dataSnapshot: DataSnapshot) {
            // TODO ...
        }
    }

    override fun onSendingMessage(senderId: String, text: String): String? {
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
        resetViewModels()

        setupViewModels()

        dataObject?.let { arena ->
            arenaObserverManager.addObserver(arenaObserver, arena)
            tryAddingRaidMeetupObserver(arena)
        }

        ShowFragmentManager.replaceFragment(ArenaInfoFragment(), childFragmentManager, R.id.arena_layout)

        return rootLayout
    }

    override fun onStart() {
        super.onStart()
        dataObject?.let { AppbarManager.setTitle(it.name) }
        AppbarManager.setMenuIcon(R.drawable.icon_menu_share) {
            shareRaidMeetup()
        }
    }

    override fun onStop() {
        AppbarManager.resetMenu()
        super.onStop()
    }

    private fun shareRaidMeetup() {

        var shareText = ""

        Kotlin.safeLet(dataObject, dataObject?.raid, raidViewModel?.raidMeetup) { arena, raid, raidMeetup ->
            shareText = createRaidPlainText(arena.name, arena.geoHash, raid, raidMeetup, raidViewModel?.participants?.value?.map { it.name })
        } ?: run {
            Log.e(TAG, "could not shared raid meetup, because arena: $dataObject, raid: ${dataObject?.raid} or raidMeetup: ${raidViewModel?.raidMeetup}")
        }

        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(sharingIntent, App.geString(R.string.arena_raid_share_raid_header)))
    }

    private fun createRaidPlainText(arenaName: String, ArenaLocation: GeoHash, raid: FirebaseRaid, raidMeetup: FirebaseRaidMeetup, participants: List<String>?): String {

        var participantsString = ""
        if (participants != null) {
            for (userName in participants) {
                participantsString += "• $userName\n"
            }
        }

        val timeEggHatches = if(raid.timestampEggHatches != TIMESTAMP_NONE) TimeCalculator.format(raid.timestampEggHatches) else DEFAULT_MEETUP_TIME
        val timeEnd = if(raid.timestampEnd != TIMESTAMP_NONE) TimeCalculator.format(raid.timestampEnd) else DEFAULT_MEETUP_TIME
        val timeMeetup = if(raidMeetup.meetupTimestamp != TIMESTAMP_NONE) TimeCalculator.format(raidMeetup.meetupTimestamp) else DEFAULT_MEETUP_TIME

        return "" +
                "🐲: ${FirebaseDefinitions.raidBossName(raid.raidBossId) ?: "---"}, ⭐️: ${raid.level}\n" +
                "🏟: $arenaName\n" +
                "⌚️: $timeEggHatches - $timeEnd\n" +
                "👫: $timeMeetup\n" +
                participantsString +
                "📍: https://maps.google.com/?q=${ArenaLocation.toLatLng().latitude},${ArenaLocation.toLatLng().longitude}"
    }

    override fun onDestroyView() {
        removeObservers()
        dataObject = null
        resetViewModels()
        super.onDestroyView()
    }

    override fun onDataChanged(dataObject: FirebaseArena?) {

        Kotlin.safeLet(activity, dataObject) { activity, arena ->
            arenaViewModel?.updateData(arena, activity)
            raidViewModel?.updateData(arena, activity)
        }

        tryAddingRaidMeetupObserver(dataObject)
    }

    /**
     * Implementation
     */

    private fun setupViewModels() {
        activity?.let {
            arenaViewModel = ViewModelProviders.of(it).get(ArenaViewModel::class.java)
            raidViewModel = ViewModelProviders.of(it).get(RaidViewModel::class.java)
            chatViewModel = ViewModelProviders.of(it).get(ChatViewModel::class.java)

            dataObject?.let { arena ->
                arenaViewModel?.updateData(arena, it)
                raidViewModel?.updateData(arena, it)
            }

            chatViewModel?.setDelegate(this)
            FirebaseUser.userData?.let { user ->
                chatViewModel?.setUser(user)
            }
        }
    }

    private fun resetViewModels() {
        activity?.let {
            ViewModelProviders.of(it).get(ArenaViewModel::class.java).reset()
            ViewModelProviders.of(it).get(RaidViewModel::class.java).reset()
            ViewModelProviders.of(it).get(ChatViewModel::class.java).reset()
        }
    }

    private fun updateRaidMeetup(newRaidMeetup: FirebaseRaidMeetup) {
        activity?.let {

            raidViewModel?.raidMeetup?.id?.let { firebase.removeChatObserver(chatObserver, it) }
            raidViewModel?.updateData(newRaidMeetup)

            if (raidViewModel?.isRaidAnnounced?.value == true) {
                firebase.addChatObserver(chatObserver, newRaidMeetup.id)
                AppbarManager.showMenu(Toolbar.MenuType.Icon)
            } else {
                AppbarManager.hideMenu(Toolbar.MenuType.Icon)
            }
        }
    }

    private fun tryAddingRaidMeetupObserver(arena: FirebaseArena?) {

        arena?.raid?.raidMeetupId?.let { raidMeetupId ->
            val raidMeetup = FirebaseRaidMeetup.new(raidMeetupId)
            firebase.addObserver(raidMeetupObserver, raidMeetup)
        }
    }

    private fun removeObservers() {

        dataObject?.let { arena ->

            arenaObserverManager.removeObserver(arenaObserver, arena)

            arena.raid?.raidMeetupId?.let { raidMeetupId ->
                val raidMeetup = FirebaseRaidMeetup.new(raidMeetupId)
                firebase.removeObserver(raidMeetupObserver, raidMeetup)
                firebase.removeChatObserver(chatObserver, raidMeetupId)
            }
        }
    }
}