package io.stanc.pogoradar.screen.arena

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.recyclerview.RecyclerViewFragment
import io.stanc.pogoradar.recyclerviewadapter.ParticipantAdapter
import io.stanc.pogoradar.viewmodel.RaidViewModel

class ParticipantsFragment: RecyclerViewFragment<FirebasePublicUser>() {
    private val TAG = javaClass.name

    override val fragmentLayoutRes: Int
        get() = R.layout.layout_participants

    override val recyclerViewIdRes: Int
        get() = R.id.participants_recyclerview

    override val orientation: Orientation
        get() = Orientation.VERTICAL

    override val initItemList: List<FirebasePublicUser>
        get() = activity?.let { ViewModelProviders.of(it).get(RaidViewModel::class.java).participants.get() ?: emptyList() } ?: emptyList()

    override fun onCreateListAdapter(context: Context, list: List<FirebasePublicUser>) = ParticipantAdapter(context, list)
}