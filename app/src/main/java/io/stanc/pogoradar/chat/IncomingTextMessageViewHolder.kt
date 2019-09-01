package io.stanc.pogoradar.chat

import android.view.View
import android.widget.TextView
import com.stfalcon.chatkit.messages.MessageHolders
import io.stanc.pogoradar.R


class IncomingTextMessageViewHolder(itemView: View, payload: Any?) :
    MessageHolders.IncomingTextMessageViewHolder<ChatMessage>(itemView, payload) {

    private val TAG = javaClass.name

    private var userName: TextView? = itemView.findViewById(R.id.messageUserName)

    override fun onBind(message: ChatMessage) {
        super.onBind(message)
        userName?.text = "${message.user.name}:"
    }
}