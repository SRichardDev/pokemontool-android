package io.stanc.pogoradar.recyclerviewadapter

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebaseQuestDefinition
import io.stanc.pogoradar.recyclerview.RecyclerViewAdapter

class QuestAdapter(private val context: Context,
                   private val quests: List<FirebaseQuestDefinition>): RecyclerViewAdapter<FirebaseQuestDefinition>(context, quests.toMutableList()) {

    override val itemLayoutRes: Int
        get() = R.layout.cardview_pokestop_quest_list_item

    override val clickableItemViewIdRes: Int
        get() = R.id.list_item_quest

    override val onlyOneItemIsSelectable: Boolean = true

    override fun onItemViewCreated(holder: Holder, id: Any) {

        quests.find { it.id == id }?.let { questDefinition ->

            val drawable = FirebaseImageMapper.questDrawable(context, questDefinition.imageName)
            holder.itemView.findViewById<ImageView>(R.id.list_item_quest_image).setImageDrawable(drawable)

            holder.itemView.findViewById<TextView>(R.id.list_item_quest_textview_name).text = questDefinition.questDescription
            holder.itemView.findViewById<TextView>(R.id.list_item_quest_textview_reward).text = questDefinition.reward
        }
    }

    fun filter(text: String) {
        val filteredQuests = mutableListOf<FirebaseQuestDefinition>()

        if (text.isEmpty()) {
            filteredQuests.addAll(quests)

        } else {

            for (quest in quests) {
                if (quest.questDescription.contains(text, ignoreCase = true) || quest.reward.contains(text, ignoreCase = true)) {
                    filteredQuests.add(quest)
                }
            }
        }

        updateList(filteredQuests)
    }
}