package io.stanc.pogoradar.chat

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.stfalcon.chatkit.messages.MessageHolders
import io.stanc.pogoradar.R
import java.lang.ref.WeakReference


class IncomingTextMessageViewHolder(itemView: View, payload: Any?) :
    MessageHolders.IncomingTextMessageViewHolder<ChatMessage>(itemView, payload) {

    private val TAG = javaClass.name

    private var userName: TextView? = itemView.findViewById(R.id.messageUserName)
    private val context = WeakReference(itemView.context)

    override fun onBind(message: ChatMessage) {
        super.onBind(message)
        userName?.text = "${message.user.name}:"

        // just workaround, because style in chat_incoming_text_message.xml will be overriden by framework of stfalcon
        context.get()?.let {
            TextViewCompat.setTextAppearance(time, R.style.TextStyle_sub)
            time.setTextColor(ContextCompat.getColor(it, R.color.chatIncomingMessageText))
        }
    }
}