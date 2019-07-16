package io.stanc.pogoradar.screen.arena

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentArenaInfoBinding
import io.stanc.pogoradar.subscreen.RaidBossFragment
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.utils.TimeCalculator
import io.stanc.pogoradar.viewmodel.ArenaViewModel
import io.stanc.pogoradar.viewmodel.RaidViewModel
import java.util.*

class ArenaInfoFragment: Fragment() {

    private var arenaViewModel: ArenaViewModel? = null
    private var raidViewModel: RaidViewModel? = null

    private var viewBinding: FragmentArenaInfoBinding? = null

    private var meetupTimeHour: Int = Calendar.getInstance().time.hours
    private var meetupTimeMinutes: Int = Calendar.getInstance().time.minutes
    private var raidBossesFragment: RaidBossFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentArenaInfoBinding.inflate(inflater, container, false)

        activity?.let {
            arenaViewModel = ViewModelProviders.of(it).get(ArenaViewModel::class.java)
            raidViewModel = ViewModelProviders.of(it).get(RaidViewModel::class.java)

            binding.arenaViewModel = arenaViewModel
            binding.raidViewModel = raidViewModel
        }

        viewBinding = binding

        updateLayout()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        meetupTimeHour = Calendar.getInstance().time.hours
        meetupTimeMinutes = Calendar.getInstance().time.minutes
    }

    /**
     * Setup
     */

    private fun setupRaidButtons(rootLayout: View) {
        Kotlin.safeLet(context, arenaViewModel?.arena) { context, arena ->

            rootLayout.findViewById<Button>(R.id.arena_raid_button_new_raid)?.let {
                it.setOnClickListener {
                    ShowFragmentManager.showFragment(RaidFragment(), fragmentManager, R.id.arena_layout)
                }
            }

            rootLayout.findViewById<Button>(R.id.arena_raid_button_raidboss)?.let {
                it.setOnClickListener {
                    raidBossesFragment?.selectedItem()?.let {
                        raidViewModel?.sendRaidBoss(it)
                    }
                }
            }

            rootLayout.findViewById<Button>(R.id.arena_raid_button_participants_list)?.let {
                it.setOnClickListener {
                    ShowFragmentManager.showFragment(ParticipantsFragment(), fragmentManager, R.id.arena_layout)
                }
            }

            rootLayout.findViewById<Switch>(R.id.arena_raid_switch_register)?.setOnCheckedChangeListener { _, isChecked ->
                raidViewModel?.let { viewModel ->

                    if (viewModel.isRaidMeetupAnnounced.get() == true) {
                        viewModel.changeParticipation(isChecked)
                    } else {
                        viewModel.changeMeetupTime(meetupTimeHour, meetupTimeMinutes)
                    }
                }
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
        arenaViewModel?.arena?.raid?.level?.let { raidBossesFragment?.showList(it) }
    }

    /**
     * viewmodel
     */
    private fun updateLayout() {
        viewBinding?.root?.apply {
            setupRaidButtons(this)
            setupTimePicker(this)
            setupRaidbossList()
        }
    }
}