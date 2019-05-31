package io.stanc.pogotool.recyclerviewadapter

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseQuestDefinition
import io.stanc.pogotool.recyclerview.RecyclerViewAdapter

class QuestAdapter(private val context: Context,
                   private val quests: List<FirebaseQuestDefinition>): RecyclerViewAdapter<FirebaseQuestDefinition>(context, quests) {

    override val itemLayoutRes: Int
        get() = R.layout.layout_list_item_quest

    override val clickableItemViewIdRes: Int
        get() = R.id.list_item_quest

    override val onlyOneItemIsSelectable: Boolean = true

    override fun onItemViewCreated(holder: Holder, position: Int) {
        val drawable = FirebaseImageMapper.questDrawable(context, quests[position].imageName)
        holder.itemView.findViewById<ImageView>(R.id.list_item_quest_image).setImageDrawable(drawable)

        holder.itemView.findViewById<TextView>(R.id.list_item_quest_textview_name).text = quests[position].quest
        holder.itemView.findViewById<TextView>(R.id.list_item_quest_textview_reward).text = quests[position].reward
    }
}