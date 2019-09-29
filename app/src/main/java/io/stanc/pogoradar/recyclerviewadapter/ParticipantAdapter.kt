package io.stanc.pogoradar.recyclerviewadapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import io.stanc.pogoradar.FirebaseImageMapper.TEAM_COLOR
import io.stanc.pogoradar.Popup
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebasePublicUser
import io.stanc.pogoradar.recyclerview.RecyclerViewAdapter
import io.stanc.pogoradar.utils.SystemUtils


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
                holder.itemView.findViewById<CardView>(R.id.cardview)?.setCardBackgroundColor(ContextCompat.getColor(context, colorInt))
            }
            holder.itemView.findViewById<TextView>(R.id.list_item_participant_level).text = participant.level.toString()
            holder.itemView.findViewById<TextView>(R.id.list_item_participant_name).text = participant.name

            setupParticipantButton(holder, participant)
        }
    }

    private fun setupParticipantButton(holder: Holder, participant: FirebasePublicUser) {

        val copyButton = holder.itemView.findViewById<View>(R.id.list_item_participant_plus)

        copyButton.visibility = if (participant.code.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
        copyButton.setOnClickListener {
            participant.code?.let { friendshipCode ->
                Popup.showToast(context, R.string.popup_raid_participant_id_copied)
                SystemUtils.copyTextToClipboard(context, friendshipCode, R.string.popup_raid_participant_id_copied)
            }
        }
    }
}