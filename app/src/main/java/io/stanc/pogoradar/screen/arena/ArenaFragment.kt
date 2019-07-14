package io.stanc.pogoradar.screen.arena

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.screen.ParcelableDataFragment
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.ArenaViewModel
import io.stanc.pogoradar.viewmodel.RaidViewModel


class ArenaFragment: ParcelableDataFragment<FirebaseArena>() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    override fun onDataChanged(dataObject: FirebaseArena?) {
        updateViewModel(dataObject)
    }

    private val arenaObserver = object: FirebaseNodeObserverManager.Observer<FirebaseArena> {

        override fun onItemChanged(item: FirebaseArena) {
            dataObject = item
        }

        override fun onItemRemoved(itemId: String) {
            dataObject = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_arena, container, false)

        dataObject?.let {
            AppbarManager.setTitle(it.name)
            firebase.addObserver(arenaObserver, it)
            updateViewModel(it)
        }

        ShowFragmentManager.replaceFragment(ArenaInfoFragment(), childFragmentManager, R.id.arena_layout)

        return rootLayout
    }

    override fun onDestroyView() {
        dataObject?.let { firebase.removeObserver(arenaObserver, it) }
        AppbarManager.reset()
        super.onDestroyView()
    }

    private fun updateViewModel(arena: FirebaseArena?) {
        activity?.let {
            ViewModelProviders.of(it).get(ArenaViewModel::class.java).updateData(arena, it)
            ViewModelProviders.of(it).get(RaidViewModel::class.java).updateData(arena, it)
        }
    }
}