package io.stanc.pogoradar.recyclerviewadapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import io.stanc.pogoradar.FirebaseImageMapper.TEAM_COLOR
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.recyclerview.RecyclerViewAdapter


class ParticipantAdapter(private val context: Context,
                         private val participants: List<FirebasePublicUser>): RecyclerViewAdapter<FirebasePublicUser>(context, participants.toMutableList()) {
    private val TAG = javaClass.name

    override val itemLayoutRes: Int
        get() = R.layout.cardview_participant_list_item

    override val clickableItemViewIdRes: Int
        get() = R.id.list_item_participant

    override val onlyOneItemIsSelectable: Boolean = true

    override fun onItemViewCreated(holder: Holder, id: Any) {

        participants.find { it.id == id }?.let { participant ->

            TEAM_COLOR[participant.team]?.let { colorInt ->
                (holder.itemView as? CardView)?.setCardBackgroundColor(ContextCompat.getColor(context, colorInt))
            }
            holder.itemView.findViewById<TextView>(R.id.list_item_participant_level).text = participant.level.toString()
            holder.itemView.findViewById<TextView>(R.id.list_item_participant_name).text = participant.name
        }
    }
}