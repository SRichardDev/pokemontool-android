package io.stanc.pogotool

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.data.FirebaseRaidboss
import java.util.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.utils.WaitingSpinner


class RaidFragment: Fragment() {

    private var firebase: FirebaseDatabase? = null
    private var listAdapter: RaidBossAdapter? = null
    private var rootLayout: View? = null

    private var eggImageButton1: ImageView? = null
    private var eggImageButton2: ImageView? = null
    private var eggImageButton3: ImageView? = null
    private var eggImageButton4: ImageView? = null
    private var eggImageButton5: ImageView? = null

    private var textEggHatched: TextView? = null

    private var raidLevel: Int? = null
    private var isEggHatched: Boolean = false
    private var participate: Boolean = false
    private var raidBossId: Int? = null
    private var timeUntilEvent: Int = 0
    private var meetupTimeHour: Int = Calendar.getInstance().time.hours
    private var meetupTimeMinutes: Int = Calendar.getInstance().time.minutes

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_raid, container, false)

        AppbarManager.setTitle(getString(R.string.raid_app_title))

        setupEggImageButtons(rootLayout)
        setupCheckBoxes(rootLayout)
        setupPicker(rootLayout)
        setupButton(rootLayout)

        loadRaidbosses()

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
                Log.d(TAG, "Debug:: checkbox raid_checkbox_egg clicked(isChecked: ${checkbox.isChecked}), textEggHatched: ${textEggHatched?.text}")
                isEggHatched = checkbox.isChecked
                textEggHatched?.text = if (isEggHatched) getString(R.string.raid_text_time_raid) else getString(R.string.raid_text_time_egg)
                Log.d(TAG, "Debug:: checkbox raid_checkbox_egg clicked, new textEggHatched: ${textEggHatched?.text}")
            }
        }

        rootLayout.findViewById<CheckBox>(R.id.raid_checkbox_participation)?.let { checkbox ->
            checkbox.setOnClickListener { participate = checkbox.isChecked }
        }
    }

    private fun setupList(rootLayout: View, firebaseRaidBosses: List<FirebaseRaidboss>) {

        rootLayout.findViewById<RecyclerView>(R.id.raid_list_raidbosses)?.let { recyclerView ->

            context?.let {
                val adapter = RaidBossAdapter(it, firebaseRaidBosses, onItemClickListener = object: RaidBossAdapter.OnItemClickListener {
                    override fun onClick(id: String) {
                        Log.i(TAG, "Debug:: setOnItemClickListener(id: $id)")
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

        // egg time

        val eggPicker = rootLayout.findViewById<NumberPicker>(R.id.raid_picker_time_egg)
        eggPicker.minValue = 0
        eggPicker.maxValue = 60
        eggPicker.value = timeUntilEvent
        eggPicker.setOnValueChangedListener { _, _, newValue ->
            timeUntilEvent = newValue
        }

        textEggHatched = rootLayout.findViewById(R.id.raid_text_egg_time)

        // meetup time

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
     * setup helper
     */

    private inner class EggOnClickListener(private val level: Int): View.OnClickListener {

        override fun onClick(p0: View) {
            resetEggImageButtonsSelectionState()
            p0.isSelected = !p0.isSelected
            raidLevel = level
            loadRaidbosses()
        }
    }

    private fun resetEggImageButtonsSelectionState() {
        eggImageButton1?.isSelected = false
        eggImageButton2?.isSelected = false
        eggImageButton3?.isSelected = false
        eggImageButton4?.isSelected = false
        eggImageButton5?.isSelected = false
    }

    /**
     * send data
     */

    // TODO: load optional sprites from: https://github.com/PokeAPI/sprites
    private fun loadRaidbosses() {
        WaitingSpinner.showProgress(R.string.spinner_title_raid_data)
        firebase?.loadRaidBosses { firebaseRaidBosses ->

            val raidBossesToShow = raidLevel?.let { chosenRaidLevel ->
                firebaseRaidBosses.filter { it.level.toInt() == chosenRaidLevel }
            } ?: kotlin.run { firebaseRaidBosses }

            rootLayout?.let { setupList(it, raidBossesToShow) }
            WaitingSpinner.hideProgress()
        }
    }

    private fun sendData() {

        listAdapter?.getSelectedItem()?.let { selectedRaidBoss ->

        }
        // TODO: check if all arguments are valid (not null...)
//        val raid = FirebaseRaid()
//        firebase?.pushRaidMeetup()
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(firebase: FirebaseDatabase): RaidFragment {
            val fragment = RaidFragment()
            fragment.firebase = firebase
            return fragment
        }
    }
}