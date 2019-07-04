package io.stanc.pogoradar.subscreen

import android.content.Context
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.FirebaseDefinitions
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.recyclerview.RecyclerViewFragment
import io.stanc.pogoradar.recyclerviewadapter.RaidBossAdapter

class RaidBossFragment: RecyclerViewFragment<FirebaseRaidbossDefinition>() {
    private val TAG = javaClass.name

    private val initRaidLevel = 3

    override val fragmentLayoutRes: Int
        get() = R.layout.layout_raidbosses

    override val recyclerViewIdRes: Int
        get() = R.id.raidbosses_recyclerview

    override val orientation: Orientation
        get() = Orientation.HORIZONTAL

    override val initItemList: List<FirebaseRaidbossDefinition>
        get() = FirebaseDefinitions.raidBosses.filter { it.level.toInt() == initRaidLevel }

    override fun onCreateListAdapter(context: Context, list: List<FirebaseRaidbossDefinition>) = RaidBossAdapter(context, list)

    fun showList(raidLevel: Int) {
        val newList = FirebaseDefinitions.raidBosses.filter { it.level.toInt() == raidLevel }
        showList(newList)
    }
}