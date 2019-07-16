package io.stanc.pogoradar.screen.pokestop

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
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.ParcelableDataFragment
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.PokestopViewModel
import io.stanc.pogoradar.viewmodel.QuestViewModel


class PokestopFragment: ParcelableDataFragment<FirebasePokestop>() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    override fun onDataChanged(dataObject: FirebasePokestop?) {
        updateViewModel(dataObject)
    }
    private val pokestopObserver = object: FirebaseNodeObserverManager.Observer<FirebasePokestop> {

        override fun onItemChanged(item: FirebasePokestop) {
            dataObject = item
        }

        override fun onItemRemoved(itemId: String) {
            dataObject = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_pokestop, container, false)

        dataObject?.let {
            firebase.addObserver(pokestopObserver, it)
            updateViewModel(it)
        }

        ShowFragmentManager.replaceFragment(PokestopInfoFragment(), childFragmentManager, R.id.pokestop_layout)

        return rootLayout
    }

    override fun onStart() {
        super.onStart()
        dataObject?.let { AppbarManager.setTitle(it.name) }
    }

    override fun onDestroyView() {
        dataObject?.let { firebase.removeObserver(pokestopObserver, it) }
        super.onDestroyView()
    }

    private fun updateViewModel(pokestop: FirebasePokestop?) {

        activity?.let {
            ViewModelProviders.of(it).get(PokestopViewModel::class.java).updateData(pokestop, it)
            ViewModelProviders.of(it).get(QuestViewModel::class.java).updateData(pokestop, it)
        }
    }
}