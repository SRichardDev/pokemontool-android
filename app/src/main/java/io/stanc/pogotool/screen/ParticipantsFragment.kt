package io.stanc.pogotool.screen

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDefinitions
import io.stanc.pogotool.firebase.node.FirebaseParticipant
import io.stanc.pogotool.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogotool.recyclerview.RecyclerViewFragment
import io.stanc.pogotool.recyclerviewadapter.RaidBossAdapter
import io.stanc.pogotool.viewmodel.RaidViewModel

class ParticipantsFragment: RecyclerViewFragment<FirebaseParticipant>() {
    private val TAG = javaClass.name

    private var viewModel: RaidViewModel? = null

    override val fragmentLayoutRes: Int
        get() = R.layout.layout_participants

    override val recyclerViewIdRes: Int
        get() = R.id.participants_recyclerview

    override val orientation: Orientation
        get() = Orientation.VERTICAL

    override val initItemList: List<FirebaseParticipant>
        get() = FirebaseDefinitions.raidBosses.filter { it.level.toInt() == initRaidLevel }

    override fun onCreateListAdapter(context: Context, list: List<FirebaseRaidbossDefinition>) = RaidBossAdapter(context, list)

    fun showList(raidLevel: Int) {
        val newList = FirebaseDefinitions.raidBosses.filter { it.level.toInt() == raidLevel }
        showList(newList)
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: RaidViewModel): ParticipantsFragment {
            val fragment = ParticipantsFragment()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}