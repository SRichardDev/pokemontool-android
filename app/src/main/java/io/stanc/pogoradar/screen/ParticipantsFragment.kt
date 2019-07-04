package io.stanc.pogoradar.screen

import android.content.Context
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.recyclerview.RecyclerViewFragment
import io.stanc.pogoradar.recyclerviewadapter.ParticipantAdapter
import io.stanc.pogoradar.viewmodel.RaidViewModel

class ParticipantsFragment: RecyclerViewFragment<FirebasePublicUser>() {
    private val TAG = javaClass.name

    private var viewModel: RaidViewModel? = null

    override val fragmentLayoutRes: Int
        get() = R.layout.layout_participants

    override val recyclerViewIdRes: Int
        get() = R.id.participants_recyclerview

    override val orientation: Orientation
        get() = Orientation.VERTICAL

    override val initItemList: List<FirebasePublicUser>
        get() = viewModel?.participants?.get() ?: emptyList()

    override fun onCreateListAdapter(context: Context, list: List<FirebasePublicUser>) = ParticipantAdapter(context, list)

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: RaidViewModel): ParticipantsFragment {
            val fragment = ParticipantsFragment()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}