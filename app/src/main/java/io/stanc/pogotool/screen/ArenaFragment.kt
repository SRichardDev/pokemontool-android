package io.stanc.pogotool.screen

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentArenaBinding
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseNodeObserverManager
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.map.MapIconFactory
import io.stanc.pogotool.subscreen.RaidBossFragment
import io.stanc.pogotool.utils.IconFactory
import io.stanc.pogotool.utils.Kotlin.safeLet
import io.stanc.pogotool.utils.ShowFragmentManager
import io.stanc.pogotool.utils.TimeCalculator
import io.stanc.pogotool.viewmodel.RaidViewModel
import java.util.*


class ArenaFragment: Fragment() {

    private var arena: FirebaseArena? = null
        set(value) {
            field = value

            updateViewModel(value)
            updateLayout()
        }


    private var firebase: FirebaseDatabase = FirebaseDatabase()
    private var viewModel: RaidViewModel? = null
    private var viewBinding: FragmentArenaBinding? = null

    private var meetupTimeHour: Int = Calendar.getInstance().time.hours
    private var meetupTimeMinutes: Int = Calendar.getInstance().time.minutes
    private var raidBossesFragment: RaidBossFragment? = null

    private val arenaObserver = object: FirebaseNodeObserverManager.Observer<FirebaseArena> {

        override fun onItemChanged(item: FirebaseArena) {
            arena = item
        }

        override fun onItemRemoved(itemId: String) {
            arena = null
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
        arena?.let { firebase.addObserver(arenaObserver, it) }
        meetupTimeHour = Calendar.getInstance().time.hours
        meetupTimeMinutes = Calendar.getInstance().time.minutes
    }

    override fun onPause() {
        arena?.let { firebase.removeObserver(arenaObserver, it) }
        AppbarManager.setTitle(getString(R.string.default_app_title))
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
        val arenaIcon = MapIconFactory.arenaIcon(context, arena, IconFactory.SizeMod.LARGE)
        imageView.setImageBitmap(arenaIcon)
    }

    private fun setupArenaInfos(rootLayout: View) {

        arena?.let { arena ->

            rootLayout.findViewById<TextView>(R.id.map_item_infos_textview_coordinates)?.let { textView ->
                val latitude = arena.geoHash.toLocation().latitude.toString()
                val longitude = arena.geoHash.toLocation().longitude.toString()
                textView.text = getString(R.string.coordinates_format, latitude, longitude)
            }

            rootLayout.findViewById<TextView>(R.id.map_item_infos_textview_added_from_user)?.let { textView ->
                textView.text = arena.submitter
            }
        }
    }

    private fun setupRaidIcon(rootLayout: View) {
        safeLet(context, arena) { context, arena ->
            rootLayout.findViewById<ImageView>(R.id.arena_raid_icon)?.let {
                setIconRaid(it, context, arena)
            }
        }
    }

    private fun setupRaidButtons(rootLayout: View) {
        safeLet(context, arena) { context, arena ->

            rootLayout.findViewById<Button>(R.id.arena_raid_button_new_raid)?.let {
                it.setOnClickListener {
                    showRaidFragment(arena)
                }
            }

            rootLayout.findViewById<Button>(R.id.arena_raid_button_raidboss)?.let {
                it.setOnClickListener {
                    raidBossesFragment?.selectedItem()?.let {
                        viewModel?.sendRaidBoss(it)
                    }
                }
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

            rootLayout.findViewById<Button>(R.id.arena_raid_button_register)?.let { button ->
                button.setOnClickListener {
                    button.isActivated = !button.isActivated
                    Log.d(TAG, "Debug:: button participant pressed, now: isActivated: ${button.isActivated}, raidAnnounced: ${viewModel?.isRaidMeetupAnnounced?.get()}")

                    viewModel?.let { viewModel ->

                        if (viewModel.isRaidMeetupAnnounced.get()!!) {
                            viewModel.changeParticipation(button.isActivated)
                        } else {
                            val meetupTime = TimeCalculator.format(meetupTimeHour, meetupTimeMinutes)
                            Log.d(TAG, "Debug:: arena_raid_button_register: meetupTime: $meetupTime <= (meetupTimeHour: $meetupTimeHour, meetupTimeMinutes: $meetupTimeMinutes)")
                            viewModel.createMeetup(meetupTime)
                        }
                    }
                }
                viewModel?.isUserParticipate?.get()?.let { button.isActivated = it }
            }

        }
    }

    private fun setupTimePicker(rootLayout: View) {

        // meetup formattedTime

        val meetupPickerHour = rootLayout.findViewById<NumberPicker>(R.id.raid_meetup_time_hour)
        meetupPickerHour.minValue = 0
        meetupPickerHour.maxValue = 23
        meetupPickerHour.value = meetupTimeHour
        meetupPickerHour.setOnValueChangedListener { _, _, newValue ->
            meetupTimeHour = newValue
        }

        val meetupPickerMinutes = rootLayout.findViewById<NumberPicker>(R.id.raid_meetup_time_minutes)
        meetupPickerMinutes.minValue = 0
        meetupPickerMinutes.maxValue = 60
        meetupPickerMinutes.value = meetupTimeMinutes
        meetupPickerMinutes.setOnValueChangedListener { _, _, newValue ->
            meetupTimeMinutes = newValue
        }
    }

    private fun setupRaidbossList() {
        raidBossesFragment = childFragmentManager.findFragmentById(R.id.fragment_raidbosses) as? RaidBossFragment
        arena?.raid?.level?.let { raidBossesFragment?.showList(it.toInt()) }
    }

    /**
     * Raid
     */

    private fun showRaidFragment(arena: FirebaseArena) {
        val fragment = RaidFragment.newInstance(arena.id, arena.geoHash)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    private fun setIconRaid(imageView: ImageView, context: Context, arena: FirebaseArena) {
        val raidDrawable = FirebaseImageMapper.raidDrawable(context, arena)
        imageView.setImageDrawable(raidDrawable)
    }

    /**
     * viewmodel
     */

    private fun updateViewModel(arena: FirebaseArena?) {
        arena?.let {
            viewModel?.updateData(arena) ?: kotlin.run {
                viewModel = RaidViewModel(it)
            }
        }
    }

    private fun updateLayout() {
        viewBinding?.root?.apply {
            setupArenaIcon(this)
            setupArenaInfos(this)
            setupRaidIcon(this)
            setupRaidButtons(this)
            setupTimePicker(this)
            setupRaidbossList()
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