package io.stanc.pogotool

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogotool.firebase.node.FirebaseRaidboss

class RaidBossFragment: Fragment() {
    private val TAG = javaClass.name

    private var listAdapter: RaidBossAdapter? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.layout_fragment_raidbosses, container, false)
        recyclerView = rootLayout.findViewById(R.id.raidbosses_recyclerview)
        showRaidBossList(raidLevel = 3)
        return rootLayout
    }

    fun showRaidBossList(raidLevel: Int) {
        val raidBossesToShow = RaidBossImageMapper.raidBosses.filter { it.level.toInt() == raidLevel }
        setupList(raidBossesToShow)
    }

    fun selectedRaidBoss(): FirebaseRaidboss? {
        return listAdapter?.getSelectedItem()
    }

    private fun setupList(firebaseRaidBosses: List<FirebaseRaidboss>) {
        recyclerView?.let { recyclerView ->

            context?.let {
                val adapter = RaidBossAdapter(
                    it,
                    firebaseRaidBosses,
                    onItemClickListener = object : RaidBossAdapter.OnItemClickListener {
                        override fun onClick(id: String) {
                            // nothing todo
                        }
                    })

                val layoutManager = LinearLayoutManager(it)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                recyclerView.layoutManager = layoutManager

                recyclerView.adapter = adapter
                listAdapter = adapter
            }
        }
    }
}