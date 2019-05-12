package io.stanc.pogotool

import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentArenaBinding
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.map.RaidBossImageMapper
import io.stanc.pogotool.utils.KotlinUtils.safeLet
import io.stanc.pogotool.utils.ShowFragmentManager
import io.stanc.pogotool.viewmodels.RaidViewModel


class ArenaFragment: Fragment() {

    private var arena: FirebaseArena? = null
        set(value) {
            field = value
            updateLayout()

            value?.let {
                viewModel?.updateData(value) ?: kotlin.run {
                    viewModel = RaidViewModel(it)
                }
            }
        }


    private var firebase: FirebaseDatabase = FirebaseDatabase()
    private var viewModel: RaidViewModel? = null
    private var viewBinding: FragmentArenaBinding? = null

    private val observer = object: FirebaseDatabase.Observer<FirebaseArena> {
        override fun onItemChanged(item: FirebaseArena) {
            arena = item
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentArenaBinding>(inflater, R.layout.fragment_arena, container, false)
        binding.viewmodel = viewModel
        viewBinding = binding

        updateLayout()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        arena?.let { AppbarManager.setTitle(it.name) }
        arena?.let { firebase.addObserver(observer, it) }
    }

    override fun onPause() {
        arena?.let { firebase.removeObserver(observer, it) }
        super.onPause()
    }

    /**
     * Setup
     */

    private fun setupArenaIcon(rootLayout: View) {
        safeLet(context, arena) { context, arena ->
            rootLayout.findViewById<ImageView>(R.id.arena_icon)?.let {
                setIconArena(it, context, arena)
            }
        }
    }

    private fun setIconArena(imageView: ImageView, context: Context, arena: FirebaseArena) {
        val iconConfig = FirebaseArena.IconConfig(iconSize = 128, innerIconSize = 64)
        val arenaIcon = arena.icon(context, iconConfig)
        imageView.setImageBitmap(arenaIcon)
    }

    private fun setupArenaInfos(rootLayout: View) {

        arena?.let { arena ->

            rootLayout.findViewById<TextView>(R.id.arena_textview_coordinates)?.let { textView ->
                val latitude = arena.geoHash.toLocation().latitude.toString()
                val longitude = arena.geoHash.toLocation().longitude.toString()
                textView.text = getString(R.string.arena_coordinates_format, latitude, longitude)
            }

            rootLayout.findViewById<TextView>(R.id.arena_textview_added_from_user)?.let { textView ->
                textView.text = arena.submitter
            }
        }
    }

    private fun setupRaidInfos(rootLayout: View) {
        safeLet(context, arena) { context, arena ->

            rootLayout.findViewById<Button>(R.id.arena_raid_button_new_raid)?.let {
                it.setOnClickListener {
                    showRaidFragment(arena)
                }
            }

            rootLayout.findViewById<ImageView>(R.id.arena_raid_icon)?.let {
                setIconRaid(it, context, arena)
            }

            rootLayout.findViewById<TextView>(R.id.arena_raid_number_participants)?.let {
                //                    it.text = arena.raid?.... TODO
            }

            rootLayout.findViewById<Button>(R.id.arena_raid_button_participants_list)?.let {
                it.setOnClickListener {
                    // TODO...
                }
            }

            rootLayout.findViewById<Button>(R.id.arena_raid_button_chat)?.let {
                it.setOnClickListener {
                    // TODO...
                }
            }

            rootLayout.findViewById<Button>(R.id.arena_raid_button_register)?.let {
                //                    it.text = TODO: is registered or not?
//                    it.isActivated = TODO: is registered or not?
                it.setOnClickListener {
                    // TODO...
                }
            }

        }
    }

    /**
     * Raid
     */

    private fun showRaidFragment(arena: FirebaseArena) {
        val fragment = RaidFragment.newInstance(arena.id, arena.geoHash)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    private fun setIconRaid(imageView: ImageView, context: Context, arena: FirebaseArena) {
        val raidDrawable = RaidBossImageMapper.raidDrawable(context, arena)
        imageView.setImageDrawable(raidDrawable)
    }

    /**
     * viewmodel
     */

    private fun updateLayout() {
        viewBinding?.root?.apply {
            setupArenaIcon(this)
            setupArenaInfos(this)
            setupRaidInfos(this)
        }
    }


    companion object {

        private val TAG = javaClass.name

        fun newInstance(arena: FirebaseArena): ArenaFragment {
            val fragment = ArenaFragment()
            fragment.arena = arena
            return fragment
        }
    }
}