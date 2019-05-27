package io.stanc.pogotool.firebase

import android.util.Log
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseQuestDefinition
import io.stanc.pogotool.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogotool.utils.WaitingSpinner

object FirebaseDefinitions {

    private val TAG = javaClass.name

    var raidBosses = listOf<FirebaseRaidbossDefinition>()
        private set
    var quests = listOf<FirebaseQuestDefinition>()
        private set

    fun loadDefinitions(firebase: FirebaseDatabase) {

        loadRaidBosses(firebase, onCompletionCallback = {
            loadQuests(firebase)
        })
    }

    // TODO: load optional sprites from: https://github.com/PokeAPI/sprites
    private fun loadRaidBosses(firebase: FirebaseDatabase, onCompletionCallback: () -> Unit = {}) {

        WaitingSpinner.showProgress(R.string.spinner_title_loading_data_1)
        firebase.loadRaidBosses { firebaseRaidBosses ->

            Log.i(TAG, "Debug:: firebaseRaidBosses: ${firebaseRaidBosses?.size}")
            firebaseRaidBosses?.let { raidBosses = it }
            WaitingSpinner.hideProgress()

            onCompletionCallback()
        }
    }

    private fun loadQuests(firebase: FirebaseDatabase, onCompletionCallback: () -> Unit = {}) {

        WaitingSpinner.showProgress(R.string.spinner_title_loading_data_2)
        firebase.loadQuests { firebaseQuests ->

            Log.i(TAG, "Debug:: firebaseQuests: ${firebaseQuests?.size}")
            firebaseQuests?.let { quests = it }
            WaitingSpinner.hideProgress()

            onCompletionCallback()
        }
    }
}