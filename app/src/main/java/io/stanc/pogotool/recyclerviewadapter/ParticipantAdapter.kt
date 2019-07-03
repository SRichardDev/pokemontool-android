package io.stanc.pogotool.recyclerviewadapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.stanc.pogotool.FirebaseImageMapper.TEAM_COLOR
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebasePublicUser
import io.stanc.pogotool.recyclerview.RecyclerViewAdapter


class ParticipantAdapter(private val context: Context,
                         private val participants: List<FirebasePublicUser>): RecyclerViewAdapter<FirebasePublicUser>(context, participants.toMutableList()) {
    private val TAG = javaClass.name

    override val itemLayoutRes: Int
        get() = R.layout.layout_list_item_participant

    override val clickableItemViewIdRes: Int
        get() = R.id.list_item_participant

    override val onlyOneItemIsSelectable: Boolean = true

    override fun onItemViewCreated(holder: Holder, id: Any) {

        participants.find { it.id == id }?.let { participant ->

            TEAM_COLOR[participant.team]?.let { colorInt ->
                holder.itemView.background = ColorDrawable(ContextCompat.getColor(context, colorInt))
            }
            holder.itemView.findViewById<TextView>(R.id.list_item_participant_level).text = participant.level.toString()
            holder.itemView.findViewById<TextView>(R.id.list_item_participant_name).text = participant.name
        }
    }
}