package io.stanc.pogotool.screen

import android.content.Context
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebasePublicUser
import io.stanc.pogotool.recyclerview.RecyclerViewFragment
import io.stanc.pogotool.recyclerviewadapter.ParticipantAdapter
import io.stanc.pogotool.viewmodel.RaidViewModel

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