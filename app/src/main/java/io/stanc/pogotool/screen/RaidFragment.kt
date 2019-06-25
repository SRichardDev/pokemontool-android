package io.stanc.pogotool.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import io.stanc.pogotool.Popup
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseRaid
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.subscreen.RaidBossFragment
import io.stanc.pogotool.utils.Kotlin
import io.stanc.pogotool.utils.TimeCalculator
import java.util.*


class RaidFragment: Fragment() {

    private var geoHash: GeoHash? = null
    private var arenaId: String? = null

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private var rootLayout: View? = null

    private var raidBossesFragment: RaidBossFragment? = null

    private var eggImage: ImageView? = null
    private var eggImageButton1: ImageView? = null
    private var eggImageButton2: ImageView? = null
    private var eggImageButton3: ImageView? = null
    private var eggImageButton4: ImageView? = null
    private var eggImageButton5: ImageView? = null

    private var eggOrRaidTimerText: TextView? = null
    private var layoutRaidParticipation: View? = null

    private var raidLevel: Int = 3
    private var isEggAlreadyHatched: Boolean = false
    private var isUserParticipating: Boolean = false
    private var timeUntilEventHour: Int = Calendar.getInstance().time.hours
    private var timeUntilEventMinutes: Int = Calendar.getInstance().time.minutes
    private var meetupTimeHour: Int = Calendar.getInstance().time.hours
    private var meetupTimeMinutes: Int = Calendar.getInstance().time.minutes

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_raid, container, false)

        setupEggImages(rootLayout)
        setupSwitches(rootLayout)
        setupTimePicker(rootLayout)
        setupButton(rootLayout)

        this.rootLayout = rootLayout
        return rootLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRaidbossList()
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(getString(R.string.raid_app_title, raidLevel))
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
    }

    private fun setupSwitches(rootLayout: View) {

        eggOrRaidTimerText = rootLayout.findViewById(R.id.raid_text_egg_time)

        layoutRaidParticipation = rootLayout.findViewById(R.id.layout_raid_participation)
        layoutRaidParticipation?.visibility = if (isUserParticipating) View.VISIBLE else View.GONE

        rootLayout.findViewById<Switch>(R.id.raid_switch_egg)?.let { switch ->
            switch.setOnCheckedChangeListener { _, isChecked ->
                isEggAlreadyHatched = isChecked
                eggOrRaidTimerText?.text = if (isEggAlreadyHatched) getString(R.string.raid_text_time_raid) else getString(
                    R.string.raid_text_time_egg
                )
            }
        }

        rootLayout.findViewById<Switch>(R.id.raid_switch_participation)?.let { switch ->
            switch.setOnCheckedChangeListener { _, isChecked ->
                isUserParticipating = isChecked
                layoutRaidParticipation?.visibility = if (isUserParticipating) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupTimePicker(rootLayout: View) {

        // egg/raid formattedTime

        val eventPickerHour = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_egg_hours)
        eventPickerHour.minValue = 0
        eventPickerHour.maxValue = 23
        eventPickerHour.value = timeUntilEventHour
        eventPickerHour.setOnValueChangedListener { _, _, newValue ->
            timeUntilEventHour = newValue
        }

        val eventPickerMinutes = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_egg_minutes)
        eventPickerMinutes.minValue = 0
        eventPickerMinutes.maxValue = 59
        eventPickerMinutes.value = timeUntilEventMinutes
        eventPickerMinutes.setOnValueChangedListener { _, _, newValue ->
            timeUntilEventMinutes = newValue
        }

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
        meetupPickerMinutes.maxValue = 59
        meetupPickerMinutes.value = meetupTimeMinutes
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

        Kotlin.safeLet(arenaId, geoHash) { arenaId, geoHash ->

            if (isEggAlreadyHatched) {

                raidBossesFragment?.selectedItem()?.id?.let {  raidbossId ->
                    sendRaid(geoHash, arenaId, raidbossId)
                } ?: kotlin.run {
                    Popup.showInfo(context, R.string.popup_info_missing_raidboss_id_title)
                }

            } else {
                sendEgg(geoHash, arenaId)
            }

        } ?: kotlin.run {
            Log.e(TAG, "tryToSendData, but arenaId: $arenaId, geoHash: $geoHash, level: $raidLevel!")
        }
    }

    private fun sendRaid(geoHash: GeoHash, arenaId: String, raidbossId: String) {
        val raid = FirebaseRaid.new(raidLevel, timeUntilEventHour, timeUntilEventMinutes, geoHash, arenaId, raidbossId)
        pushRaidAndMeetupIfUserParticipates(raid)
        closeScreen()
    }

    private fun sendEgg(geoHash: GeoHash, arenaId: String) {
        val raid = FirebaseRaid.new(raidLevel, timeUntilEventHour, timeUntilEventMinutes, geoHash, arenaId)
        pushRaidAndMeetupIfUserParticipates(raid)
        closeScreen()
    }

    private fun pushRaidAndMeetupIfUserParticipates(raid: FirebaseRaid) {

        val raidMeetup = if (isUserParticipating) {
            val meetupTime = TimeCalculator.format(meetupTimeHour, meetupTimeMinutes)
            FirebaseRaidMeetup("", meetupTime, participantUserIds = emptyList(), chat = emptyList())
        } else {
            null
        }

        firebase.pushRaid(raid, raidMeetup)
    }

    private fun closeScreen() {
        fragmentManager?.findFragmentByTag(this::class.java.name)?.let {
            fragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
    }

    private fun formattedTime(hours: Int, minutes: Int): String = "$hours:$minutes"

    companion object {

        private val TAG = javaClass.name

        fun newInstance(arenaId: String, geoHash: GeoHash): RaidFragment {
            val fragment = RaidFragment()
            fragment.arenaId = arenaId
            fragment.geoHash = geoHash
            return fragment
        }
    }
}