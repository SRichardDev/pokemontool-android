package io.stanc.pogotool.recyclerviewadapter

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseQuestDefinition
import io.stanc.pogotool.recyclerview.RecyclerViewAdapter

class QuestAdapter(private val context: Context,
                   private val quests: List<FirebaseQuestDefinition>): RecyclerViewAdapter<FirebaseQuestDefinition>(context, quests.toMutableList()) {

    override val itemLayoutRes: Int
        get() = R.layout.layout_list_item_quest

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