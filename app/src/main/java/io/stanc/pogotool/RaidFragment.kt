package io.stanc.pogotool

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseRaidboss
import java.util.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseRaid
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.RaidBossImageMapper
import io.stanc.pogotool.utils.KotlinUtils


class RaidFragment: Fragment() {

    private var firebase: FirebaseDatabase? = null
    private var geoHash: GeoHash? = null
    private var arenaId: String? = null
    private var listAdapter: RaidBossAdapter? = null
    private var rootLayout: View? = null

    private var eggImageButton1: ImageView? = null
    private var eggImageButton2: ImageView? = null
    private var eggImageButton3: ImageView? = null
    private var eggImageButton4: ImageView? = null
    private var eggImageButton5: ImageView? = null

    private var textEggHatched: TextView? = null
    private var layoutRaidBosses: View? = null

    private var raidLevel: Int? = null
    private var isEggAlreadyHatched: Boolean = false
    private var isUserParticipating: Boolean = false
    private var timeUntilEvent: Int = 0
    private var meetupTimeHour: Int = Calendar.getInstance().time.hours
    private var meetupTimeMinutes: Int = Calendar.getInstance().time.minutes

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_raid, container, false)

        AppbarManager.setTitle(getString(R.string.raid_app_title))

        setupEggImageButtons(rootLayout)
        setupCheckBoxes(rootLayout)
        setupRaidbossList(rootLayout)
        setupPicker(rootLayout)
        setupButton(rootLayout)

        this.rootLayout = rootLayout
        return rootLayout
    }

    /**
     * setup
     */

    private fun setupEggImageButtons(rootLayout: View) {

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

    private fun setupCheckBoxes(rootLayout: View) {

        rootLayout.findViewById<CheckBox>(R.id.raid_checkbox_egg)?.let { checkbox ->
            checkbox.setOnClickListener {
                isEggAlreadyHatched = checkbox.isChecked
                textEggHatched?.text = if (isEggAlreadyHatched) getString(R.string.raid_text_time_raid) else getString(R.string.raid_text_time_egg)
                layoutRaidBosses?.visibility = if (isEggAlreadyHatched) View.VISIBLE else View.GONE
            }
        }

        rootLayout.findViewById<CheckBox>(R.id.raid_checkbox_participation)?.let { checkbox ->
            checkbox.setOnClickListener { isUserParticipating = checkbox.isChecked }
        }
    }

    private fun setupRaidbossList(rootLayout: View) {
        Log.i(TAG, "Debug:: setupRaidbossList, raidLevel: $raidLevel, list: ${RaidBossImageMapper.raidBosses.size}, rootLayout: $rootLayout")

        val raidBossesToShow = raidLevel?.let { chosenRaidLevel ->
            RaidBossImageMapper.raidBosses.filter { it.level.toInt() == chosenRaidLevel }

        } ?: kotlin.run {
            RaidBossImageMapper.raidBosses
        }

        setupList(rootLayout, raidBossesToShow)
    }

    private fun setupList(rootLayout: View, firebaseRaidBosses: List<FirebaseRaidboss>) {
        rootLayout.findViewById<RecyclerView>(R.id.raid_list_raidbosses)?.let { recyclerView ->

            layoutRaidBosses = recyclerView

            context?.let {
                val adapter = RaidBossAdapter(it, firebaseRaidBosses, onItemClickListener = object: RaidBossAdapter.OnItemClickListener {
                    override fun onClick(id: String) {
                        listAdapter?.getSelectedItem()?.level?.toInt()?.let { raidLevel ->
                            selectEggImageButton(raidLevel)
                        }
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

    private fun setupPicker(rootLayout: View) {

        // egg formattedTime

        val eggPicker = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_egg)
        eggPicker.minValue = 0
        eggPicker.maxValue = 60
        eggPicker.value = timeUntilEvent
        eggPicker.setOnValueChangedListener { _, _, newValue ->
            timeUntilEvent = newValue
        }

        textEggHatched = rootLayout.findViewById(R.id.raid_text_egg_time)

        // meetup formattedTime

        val meetupPickerHour = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_meetup_1)
        meetupPickerHour.minValue = 0
        meetupPickerHour.maxValue = 23
        meetupPickerHour.value = meetupTimeHour
        meetupPickerHour.setOnValueChangedListener { _, _, newValue ->
            meetupTimeHour = newValue
        }

        val meetupPickerMinutes = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_meetup_2)
        meetupPickerMinutes.minValue = 0
        meetupPickerMinutes.maxValue = 60
        meetupPickerMinutes.value = meetupTimeMinutes
        meetupPickerMinutes.setOnValueChangedListener { _, _, newValue ->
            meetupTimeMinutes = newValue
        }
    }

    private fun setupButton(rootLayout: View) {
        rootLayout.findViewById<Button>(R.id.raid_button_send)?.setOnClickListener {
            sendData()
        }
    }

    /**
     * egg button selection
     */

    private inner class EggOnClickListener(private val level: Int): View.OnClickListener {

        override fun onClick(p0: View) {
            switchEggImageButtonSelection(level)
            rootLayout?.let { setupRaidbossList(it) }
        }
    }

    private fun switchEggImageButtonSelection(level: Int) {
        when(level) {
            1 -> eggImageButton1?.let { if (it.isSelected) deselectEggImageButton(level) else selectEggImageButton(level) }
            2 -> eggImageButton2?.let { if (it.isSelected) deselectEggImageButton(level) else selectEggImageButton(level) }
            3 -> eggImageButton3?.let { if (it.isSelected) deselectEggImageButton(level) else selectEggImageButton(level) }
            4 -> eggImageButton4?.let { if (it.isSelected) deselectEggImageButton(level) else selectEggImageButton(level) }
            5 -> eggImageButton5?.let { if (it.isSelected) deselectEggImageButton(level) else selectEggImageButton(level) }
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
                raidLevel = null
            }
            2 -> eggImageButton2?.let {
                it.isSelected = false
                raidLevel = null
            }
            3 -> eggImageButton3?.let  {
                it.isSelected = false
                raidLevel = null
            }
            4 -> eggImageButton4?.let {
                it.isSelected = false
                raidLevel = null
            }
            5 -> eggImageButton5?.let {
                it.isSelected = false
                raidLevel = null
            }
        }
    }

    /**
     * send data
     */

    private fun sendData() {

        KotlinUtils.safeLet(arenaId, geoHash, raidLevel) { arenaId, geoHash, raidLevel ->

            if (isEggAlreadyHatched) {

                val raidbossId = listAdapter?.getSelectedItem()?.id
                val raid = FirebaseRaid.new(raidLevel, timeUntilEvent, geoHash, arenaId, raidbossId)
                pushRaidAndMeetupIfUserParticipates(raid)

            } else {

                val raid = FirebaseRaid.new(raidLevel, timeUntilEvent, geoHash, arenaId)
                pushRaidAndMeetupIfUserParticipates(raid)
            }

            closeScreen()

        } ?: kotlin.run { Log.e(TAG, "sendData, but arenaId: $arenaId, geoHash: $geoHash, level: $raidLevel!") }
    }

    private fun pushRaidAndMeetupIfUserParticipates(raid: FirebaseRaid) {

        val raidMeetup = getMeetupIfUserParticipates()
        firebase?.pushRaid(raid, raidMeetup)
    }

    private fun getMeetupIfUserParticipates(): FirebaseRaidMeetup? {

        return if (isUserParticipating) {

            FirebaseUser.userData?.id?.let {

                val participants: List<String> = listOf(it)
                FirebaseRaidMeetup("",  formattedTime(meetupTimeHour, meetupTimeMinutes), participants)

            } ?: kotlin.run {
                Log.e(TAG, "could not send raid meetup, because user is logged out. (userData: ${FirebaseUser.userData}, userData?.id: ${FirebaseUser.userData?.id})")
                null
            }

        } else {
            null
        }
    }

    private fun closeScreen() {
        fragmentManager?.findFragmentByTag(this::class.java.name)?.let {
            fragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
    }

    private fun formattedTime(hours: Int, minutes: Int): String = "$hours:$minutes"

    companion object {

        private val TAG = javaClass.name

        fun newInstance(firebase: FirebaseDatabase, arenaId: String, geoHash: GeoHash): RaidFragment {
            val fragment = RaidFragment()
            fragment.firebase = firebase
            fragment.arenaId = arenaId
            fragment.geoHash = geoHash
            return fragment
        }
    }
}