package io.stanc.pogoradar.viewmodel.pokestop

import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.databinding.ObservableField
import android.graphics.drawable.Drawable
import android.util.Log
import io.stanc.pogoradar.App
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.FirebaseDefinitions
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.utils.TimeCalculator.isCurrentDay

class QuestViewModel : ViewModel() {

    private val TAG = javaClass.name

    /**
     * observable fields
     */

    var pokestop: FirebasePokestop? = null
    val questExists = ObservableField<Boolean>(false)
    val quest = ObservableField<String>(App.geString(R.string.pokestop_quest_none))
    val reward = ObservableField<String>(App.geString(R.string.pokestop_quest_none))
    val questImage = ObservableField<Drawable?>()

    fun updateData(pokestop: FirebasePokestop?, context: Context) {
        this.pokestop = pokestop

        pokestop?.quest?.let { firebaseQuest ->

            FirebaseDefinitions.quests.firstOrNull { it.id == firebaseQuest.definitionId }?.let { questDefinition ->

                val validQuest = (firebaseQuest.timestamp as? Long)?.let { timestamp ->
                    isCurrentDay(timestamp)
                } ?: run {
                    false
                }

                questExists.set(validQuest)
                quest.set(questDefinition.questDescription)
                reward.set(questDefinition.reward)
                questImage.set(imageDrawable(questDefinition.imageName, context))

            } ?: run {
                Log.w(TAG, "Quest data exists but definitionsId ${firebaseQuest.definitionId} not found in local quests: ${FirebaseDefinitions.quests}")
                resetData()
            }

        } ?: run {
            resetData()
        }
    }

    private fun resetData() {
        questExists.set(false)
        quest.set(App.geString(R.string.pokestop_quest_none))
        reward.set(App.geString(R.string.pokestop_quest_none))
        questImage.set(null)
    }

    private fun imageDrawable(imageName: String, context: Context): Drawable? {
        return FirebaseImageMapper.questDrawable(context, imageName)
    }

    companion object {

        fun new(pokestop: FirebasePokestop, context: Context): QuestViewModel {
            val viewModel = QuestViewModel()
            viewModel.updateData(pokestop, context)
            return viewModel
        }
    }
}