package io.stanc.pogoradar.screen.arena

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.Popup
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.databinding.FragmentRaidBinding
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.node.FirebaseRaid
import io.stanc.pogoradar.firebase.node.FirebaseRaidMeetup
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.subscreen.RaidBossFragment
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.SegmentedControlView
import io.stanc.pogoradar.utils.TimeCalculator
import io.stanc.pogoradar.viewmodel.arena.ArenaViewModel
import io.stanc.pogoradar.viewmodel.arena.RaidCreationViewModel
import java.util.Calendar


class RaidFragment: Fragment() {
    private val TAG = javaClass.name

    private var arenaViewModel: ArenaViewModel? = null
    private var raidCreationViewModel: RaidCreationViewModel? = null

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private var raidBossesFragment: RaidBossFragment? = null

    private var eggImage: ImageView? = null
    private var eggImageButton1: ImageView? = null
    private var eggImageButton2: ImageView? = null
    private var eggImageButton3: ImageView? = null
    private var eggImageButton4: ImageView? = null
    private var eggImageButton5: ImageView? = null

    private var eggOrRaidTimerText: TextView? = null
    private var raidBossesTitle: TextView? = null
    private var raidTimePickerHour: NumberPicker? = null

    private var raidLevel: Int = 3
    private var isEggAlreadyHatched: Boolean = false
    private var timeUntilEventHour: Int = -1
    private var timeUntilEventMinutes: Int = 0
    private var meetupTimeHour: Int = 0
    private var meetupTimeMinutes: Int = 0

    private val onTimeSelectionChangeListener = object : SegmentedControlView.OnSelectionChangeListener {

        override fun onSelectionChange(selection: SegmentedControlView.Selection) {
            when(selection) {
                SegmentedControlView.Selection.LEFT -> {
                    raidTimePickerHour?.visibility = View.VISIBLE
                }
                SegmentedControlView.Selection.RIGHT -> {
                    raidTimePickerHour?.visibility = View.GONE
                    timeUntilEventHour = -1
                }
                else -> Log.w(TAG, "unsupported selection: ${selection.name} for layout raid_segmentedcontrolview_time_hour_minutes")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentRaidBinding.inflate(inflater, container, false)

        activity?.let {
            arenaViewModel = ViewModelProviders.of(it).get(ArenaViewModel::class.java)
        }

        raidCreationViewModel = ViewModelProviders.of(this).get(RaidCreationViewModel::class.java)

        binding.raidCreationViewModel = raidCreationViewModel
        binding.lifecycleOwner = this

        binding.root.findViewById<TextView>(R.id.arena_title)?.text = arenaViewModel?.arena?.name

        setupEggImages(binding.root)
        setupSwitches(binding.root)
        setupTimePicker(binding.root)
        setupButton(binding.root)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRaidbossList()
    }

    override fun onStart() {
        super.onStart()
        AppbarManager.setTitle(getString(R.string.raid_app_title, raidLevel))
    }

    override fun onResume() {
        super.onResume()
        updateLayoutsForRaidLevel()
        selectEggImageButton(raidLevel)
        meetupTimeHour = Calendar.getInstance().time.hours
        meetupTimeMinutes = Calendar.getInstance().time.minutes
    }

    /**
     * setup
     */

    private fun updateLayoutsForRaidLevel() {

        AppbarManager.setTitle(getString(R.string.raid_app_title, raidLevel))

        when(raidLevel) {
            1 -> eggImage?.setImageResource(R.drawable.icon_level_1_30dp)
            2 -> eggImage?.setImageResource(R.drawable.icon_level_2_30dp)
            3 -> eggImage?.setImageResource(R.drawable.icon_level_3_30dp)
            4 -> eggImage?.setImageResource(R.drawable.icon_level_4_30dp)
            5 -> eggImage?.setImageResource(R.drawable.icon_level_5_30dp)
        }
    }

    private fun setupEggImages(rootLayout: View) {

        eggImage = rootLayout.findViewById(R.id.raid_egg)

        eggImageButton1 = rootLayout.findViewById(R.id.raid_egg_1)
        eggImageButton1?.setOnClickListener(EggOnClickListener(1))

        eggImageButton2 = rootLayout.findViewById(R.id.raid_egg_2)
        eggImageButton2?.setOnClickListener(EggOnClickListener(2))

        eggImageButton3 = rootLayout.findViewById(R.id.raid_egg_3)
        eggImageButton3?.setOnClickListener(EggOnClickListener(3))

        eggImageButton4 = rootLayout.findViewById(R.id.raid_egg_4)
        eggImageButton4?.setOnClickListener(EggOnClickListener(4))

        eggImageButton5 = rootLayout.findViewById(R.id.raid_egg_5)
        eggImageButton5?.setOnClickListener(EggOnClickListener(5))
    }

    private fun setupRaidbossList() {
        raidBossesFragment = childFragmentManager.findFragmentById(R.id.fragment_raidbosses) as? RaidBossFragment
        raidBossesFragment?.enableSelectItem(false)
    }

    private fun setupSwitches(rootLayout: View) {

        eggOrRaidTimerText = rootLayout.findViewById(R.id.raid_text_egg_time)
        raidBossesTitle = rootLayout.findViewById(R.id.raidbosses_title)

        rootLayout.findViewById<Switch>(R.id.raid_switch_egg)?.let { switch ->
            switch.setOnCheckedChangeListener { _, isChecked ->
                isEggAlreadyHatched = isChecked
                eggOrRaidTimerText?.text = if (isEggAlreadyHatched) getString(R.string.raid_text_time_raid) else getString(R.string.raid_text_time_egg)
                raidBossesTitle?.text = if (isEggAlreadyHatched) getString(R.string.raid_raidboss_selection) else getString(R.string.raid_raidboss_overview)
                raidBossesFragment?.deselectAllItems()
                raidBossesFragment?.enableSelectItem(isEggAlreadyHatched)
            }
        }

        rootLayout.findViewById<SegmentedControlView>(R.id.raid_segmentedcontrolview_time_hour_minutes)?.let { segmentedControlView ->

            segmentedControlView.setSegment(getString(R.string.raid_button_time_hour), SegmentedControlView.Selection.LEFT)
            segmentedControlView.setSegment(getString(R.string.raid_button_time_minutes), SegmentedControlView.Selection.RIGHT)
            segmentedControlView.setOnSelectionChangeListener(onTimeSelectionChangeListener)
        }
    }

    private fun setupTimePicker(rootLayout: View) {

        // egg/raid formattedTime

        raidTimePickerHour = rootLayout.findViewById(R.id.raid_picker_time_egg_hours)
        raidTimePickerHour?.minValue = 0
        raidTimePickerHour?.maxValue = 23
        raidTimePickerHour?.value = Calendar.getInstance().time.hours
        raidTimePickerHour?.setOnValueChangedListener { _, _, newValue ->
            timeUntilEventHour = newValue
        }

        val eventPickerMinutes = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_egg_minutes)
        eventPickerMinutes.minValue = 0
        eventPickerMinutes.maxValue = 59
        eventPickerMinutes.value = Calendar.getInstance().time.minutes
        eventPickerMinutes.setOnValueChangedListener { _, _, newValue ->
            timeUntilEventMinutes = newValue
        }

        // meetup formattedTime

        val meetupPickerHour = rootLayout.findViewById<NumberPicker>(R.id.raid_meetup_time_hour)
        meetupPickerHour.minValue = 0
        meetupPickerHour.maxValue = 23
        meetupPickerHour.value = Calendar.getInstance().time.hours
        meetupPickerHour.setOnValueChangedListener { _, _, newValue ->
            meetupTimeHour = newValue
        }

        val meetupPickerMinutes = rootLayout.findViewById<NumberPicker>(R.id.raid_meetup_time_minutes)
        meetupPickerMinutes.minValue = 0
        meetupPickerMinutes.maxValue = 59
        meetupPickerMinutes.value = Calendar.getInstance().time.minutes
        meetupPickerMinutes.setOnValueChangedListener { _, _, newValue ->
            meetupTimeMinutes = newValue
        }
    }

    private fun setupButton(rootLayout: View) {
        rootLayout.findViewById<Button>(R.id.raid_button_send)?.setOnClickListener {
            tryToSendData()
        }
    }

    /**
     * egg button selection
     */

    private inner class EggOnClickListener(private val level: Int): View.OnClickListener {

        override fun onClick(p0: View) {
            deselectAllEggImageButtons(butLevel=level)
            selectEggImageButton(level)
            raidBossesFragment?.showList(level)
        }
    }

    private fun selectEggImageButton(level: Int) {
        deselectAllEggImageButtons(butLevel = level)

        when(level) {
            1 -> eggImageButton1?.let {
                it.isSelected = true
                raidLevel = level
            }
            2 -> eggImageButton2?.let {
                it.isSelected = true
                raidLevel = level
            }
            3 -> eggImageButton3?.let  {
                it.isSelected = true
                raidLevel = level
            }
            4 -> eggImageButton4?.let {
                it.isSelected = true
                raidLevel = level
            }
            5 -> eggImageButton5?.let {
                it.isSelected = true
                raidLevel = level
            }
        }

        updateLayoutsForRaidLevel()
    }

    private fun deselectAllEggImageButtons(butLevel: Int) {
        if (butLevel != 1) deselectEggImageButton(1)
        if (butLevel != 2) deselectEggImageButton(2)
        if (butLevel != 3) deselectEggImageButton(3)
        if (butLevel != 4) deselectEggImageButton(4)
        if (butLevel != 5) deselectEggImageButton(5)
    }

    private fun deselectEggImageButton(level: Int) {
        when(level) {
            1 -> eggImageButton1?.let {
                it.isSelected = false
            }
            2 -> eggImageButton2?.let {
                it.isSelected = false
            }
            3 -> eggImageButton3?.let  {
                it.isSelected = false
            }
            4 -> eggImageButton4?.let {
                it.isSelected = false
            }
            5 -> eggImageButton5?.let {
                it.isSelected = false
            }
        }
    }

    /**
     * send data
     */

    private fun tryToSendData() {

        Kotlin.safeLet(arenaViewModel?.arena?.id, arenaViewModel?.arena?.geoHash) { arenaId, geoHash ->

            if (isEggAlreadyHatched) {

                raidBossesFragment?.selectedItem()?.id?.let {  raidbossId ->
                    sendRaid(geoHash, arenaId, raidbossId)
                } ?: run {
                    Popup.showInfo(context, R.string.popup_info_missing_raidboss_id_title)
                }

            } else {
                sendEgg(geoHash, arenaId)
            }

        } ?: run {
            Log.e(TAG, "tryToSendData, but arenaViewModel?.arena: ${arenaViewModel?.arena}, level: $raidLevel!")
        }
    }

    private fun sendRaid(geoHash: GeoHash, arenaId: String, raidbossId: String) {
//        Log.d(TAG, "Debug:: sendRaid($geoHash, $arenaId), ($timeUntilEventHour:$timeUntilEventMinutes)")

        val raid = if (timeUntilEventHour == -1) {
            FirebaseRaid.new(raidLevel, timeUntilEventMinutes, geoHash, arenaId, raidbossId)
        } else {
            FirebaseRaid.new(raidLevel, timeUntilEventHour, timeUntilEventMinutes, geoHash, arenaId, raidbossId)
        }

        pushRaidAndMeetup(raid)
        closeScreen()
    }

    private fun sendEgg(geoHash: GeoHash, arenaId: String) {
//        Log.d(TAG, "Debug:: sendEgg($geoHash, $arenaId), ($timeUntilEventHour:$timeUntilEventMinutes)")

        val raid = if (timeUntilEventHour == -1) {
            FirebaseRaid.new(raidLevel, timeUntilEventMinutes, geoHash, arenaId)
        } else {
            FirebaseRaid.new(raidLevel, timeUntilEventHour, timeUntilEventMinutes, geoHash, arenaId)
        }

        pushRaidAndMeetup(raid)
        closeScreen()
    }

    private fun pushRaidAndMeetup(raid: FirebaseRaid) {

        val meetupTime = if (raidCreationViewModel?.isUserParticipate?.value == true) {
            TimeCalculator.format(meetupTimeHour, meetupTimeMinutes)
        } else {
            DatabaseKeys.DEFAULT_MEETUP_TIME
        }

        val raidMeetup = FirebaseRaidMeetup.new(meetupTime)
        firebase.pushRaid(raid, raidMeetup)
    }

    private fun closeScreen() {
        fragmentManager?.findFragmentByTag(this::class.java.name)?.let {
            fragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
    }
}