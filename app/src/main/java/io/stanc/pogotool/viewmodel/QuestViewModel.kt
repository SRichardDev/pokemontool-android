package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.databinding.ObservableField
import android.graphics.drawable.Drawable
import android.util.Log
import io.stanc.pogotool.App
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDefinitions
import io.stanc.pogotool.firebase.node.FirebasePokestop

class QuestViewModel(private var pokestop: FirebasePokestop): ViewModel() {

    private val TAG = javaClass.name

    /**
     * observable fields
     */

    val questExists = ObservableField<Boolean>(false)
    val quest = ObservableField<String>(App.geString(R.string.pokestop_quest_none))
    val reward = ObservableField<String>(App.geString(R.string.pokestop_quest_none))
    private var imageName: String? = null

    init {
        updateData(pokestop)
    }

    fun imageDrawable(context: Context): Drawable? {

        return imageName?.let {
            FirebaseImageMapper.questDrawable(context, it)
        } ?: kotlin.run {
            null
        }
    }

    fun updateData(pokestop: FirebasePokestop) {
        Log.i(TAG, "Debug:: updateData($pokestop)")

        pokestop.quest?.let { firebaseQuest ->

            FirebaseDefinitions.quests.firstOrNull { it.id == firebaseQuest.definitionId }?.let { questDefinition ->

                questExists.set(true)
                quest.set(questDefinition.quest)
                reward.set(questDefinition.reward)
                imageName = questDefinition.imageName

            } ?: kotlin.run {
                Log.w(TAG, "Quest data exists but definitionsId ${firebaseQuest.definitionId} not found in local quests: ${FirebaseDefinitions.quests}")
                resetData()
            }

        } ?: kotlin.run {
            Log.d(TAG, "Debug:: No quest defined in pokestop: $pokestop")
            resetData()
        }

        this.pokestop = pokestop
    }

    private fun resetData() {
        questExists.set(false)
        quest.set(App.geString(R.string.pokestop_quest_none))
        reward.set(App.geString(R.string.pokestop_quest_none))
        imageName = null
    }
}