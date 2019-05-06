package io.stanc.pogotool

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebaseRaid.RaidState
import io.stanc.pogotool.map.RaidBossImageMapper
import io.stanc.pogotool.utils.KotlinUtils.safeLet

class ArenaFragment: Fragment() {

    private var firebase: FirebaseDatabase? = null
    private var arena: FirebaseArena? = null

    private var rootLayout: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_arena, container, false)

        arena?.let { AppbarManager.setTitle(it.name) }

        setupArenaIcon(rootLayout)
        setupArenaInfos(rootLayout)
        setupArenaRaidIfRunning(rootLayout)

        this.rootLayout = rootLayout
        return rootLayout
    }

    /**
     * Setup
     */

    private fun setupArenaIcon(rootLayout: View) {
        safeLet(context, arena) { context, arena ->

            val imageView = rootLayout.findViewById<ImageView>(R.id.arena_textview_coordinates)
            setIconArena(imageView, context, arena)
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

    private fun setupArenaRaidIfRunning(rootLayout: View) {
        if (isRaidRunning()) {

            rootLayout.findViewById<View>(R.id.arena_layout_raid)?.visibility = View.VISIBLE

            safeLet(context, arena) { context, arena ->

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
    }

    /**
     * Raid
     */

    private fun isRaidRunning(): Boolean {

        return arena?.raid?.let { raid ->

            raid.currentRaidState() == RaidState.RAID_RUNNING

        } ?: kotlin.run { false }
    }

    private fun setIconRaid(imageView: ImageView, context: Context, arena: FirebaseArena) {
        val raidDrawable = RaidBossImageMapper.raidDrawable(context, arena)
        imageView.setImageDrawable(raidDrawable)
    }


    companion object {

        private val TAG = javaClass.name

        fun newInstance(firebase: FirebaseDatabase, arena: FirebaseArena): ArenaFragment {
            val fragment = ArenaFragment()
            fragment.firebase = firebase
            fragment.arena = arena
            return fragment
        }
    }
}