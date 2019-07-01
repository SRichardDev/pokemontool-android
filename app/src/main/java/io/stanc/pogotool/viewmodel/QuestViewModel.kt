package io.stanc.pogotool.viewmodel

import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.databinding.ObservableField
import android.graphics.drawable.Drawable
import android.util.Log
import io.stanc.pogotool.App
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDefinitions
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.utils.TimeCalculator.currentDay

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
        } ?: run {
            null
        }
    }

    fun updateData(pokestop: FirebasePokestop) {

        pokestop.quest?.let { firebaseQuest ->

            FirebaseDefinitions.quests.firstOrNull { it.id == firebaseQuest.definitionId }?.let { questDefinition ->

                val validQuest = (firebaseQuest.timestamp as? Long)?.let { timestamp ->
                    currentDay(timestamp)
                } ?: run { false }

                questExists.set(validQuest)
                quest.set(questDefinition.questDescription)
                reward.set(questDefinition.reward)
                imageName = questDefinition.imageName

            } ?: run {
                Log.w(TAG, "Quest data exists but definitionsId ${firebaseQuest.definitionId} not found in local quests: ${FirebaseDefinitions.quests}")
                resetData()
            }

        } ?: run {
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