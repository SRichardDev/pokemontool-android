package io.stanc.pogoradar.firebase

import android.util.Log
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebaseQuestDefinition
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.utils.DelayedTrigger
import io.stanc.pogoradar.utils.WaitingSpinner

object FirebaseDefinitions {

    private val TAG = javaClass.name

    var raidBosses = listOf<FirebaseRaidbossDefinition>()
        private set
    var quests = listOf<FirebaseQuestDefinition>()
        private set

    fun loadDefinitions(firebase: FirebaseDatabase) {

        loadRaidBosses(firebase, onCompletionCallback = {
            loadQuests(firebase, onCompletionCallback = {
            })
        })
    }

    fun raidBossName(raidBossId: String?): String? {
        return raidBosses.firstOrNull {it.id == raidBossId}?.name
    }

    // TODO: load optional sprites from: https://github.com/PokeAPI/sprites
    private fun loadRaidBosses(firebase: FirebaseDatabase, onCompletionCallback: () -> Unit = {}) {

        firebase.loadRaidBosses { firebaseRaidBosses ->

            Log.i(TAG, "loaded raidBosses: ${firebaseRaidBosses?.size}")
            firebaseRaidBosses?.let { raidBosses = it }
            onCompletionCallback()
        }
    }

    private fun loadQuests(firebase: FirebaseDatabase, onCompletionCallback: () -> Unit = {}) {

        firebase.loadQuests { firebaseQuests ->

            Log.i(TAG, "loaded quests: ${firebaseQuests?.size}")
            firebaseQuests?.let { quests = it }
            onCompletionCallback()
        }
    }
}