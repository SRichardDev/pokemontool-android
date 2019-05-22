package io.stanc.pogotool.viewmodels

import android.arch.lifecycle.ViewModel
import android.util.Log
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.firebase.node.FirebaseQuestDefinition
import io.stanc.pogotool.firebase.node.FirebaseRaidMeetup

class QuestViewModel(private var pokestop: FirebasePokestop): ViewModel() {

    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()
    private var questDefinition: FirebaseQuestDefinition? = null

    init {
        updateData(pokestop)
    }

    fun updateData(pokestop: FirebasePokestop) {
        Log.i(TAG, "Debug:: updateData($pokestop)")

        requestRaidMeetupData(arena)

        pokestop.quest?.let { quest ->
            //            isRaidAnnounced.set(raid.currentRaidState() != FirebaseRaid.RaidState.NONE)
//            raidState.set(raid.currentRaidState())
//            raidTime.set(raidTime(raid))
//            isRaidBossMissing.set(isRaidAnnounced.get() == true && raidState.get() == RaidState.RAID_RUNNING && raid.raidBossId == null)

        } ?: kotlin.run {
            //            isRaidAnnounced.set(false)
//            raidState.set(FirebaseRaid.RaidState.NONE)
//            raidTime.set(App.geString(R.string.arena_raid_time_none))
//            isRaidBossMissing.set(false)
        }

        this.pokestop = pokestop
    }
}