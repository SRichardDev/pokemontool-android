package io.stanc.pogoradar.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import io.stanc.pogoradar.Popup
import io.stanc.pogoradar.R


class ChatFragment: Fragment(),
    MessagesListAdapter.SelectionListener,
    MessagesListAdapter.OnLoadMoreListener,
    MessageInput.InputListener,
    MessageInput.AttachmentsListener,
    MessageInput.TypingListener {

    private val TAG = javaClass.name

    // TODO: no avatar images yet
    private val imageLoader: ImageLoader? = null

    private val DEFAULT_MESSAGE_ID: String = "defaultMessageId"
    private var messagesList: MessagesList? = null
    private var messagesAdapter: MessagesListAdapter<ChatMessage>? = null

    private var viewModel: ChatViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_chat, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(ChatViewModel::class.java)
        }

        setupChatMessageList(rootLayout)
        setupChatInput(rootLayout)

        return rootLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel?.messages?.let { showMessages(it) }
    }

    private fun setupChatMessageList(rootLayout: View) {

        messagesList = rootLayout.findViewById(R.id.messagesList)

        val messageConfig = MessageHolders()
            .setIncomingTextConfig(IncomingTextMessageViewHolder::class.java, R.layout.chat_incoming_text_message)
            .setOutcomingTextLayout(R.layout.chat_outgoing_text_message)
            .setIncomingImageLayout(R.layout.chat_incoming_image_message)
            .setOutcomingImageLayout(R.layout.chat_outgoing_image_message)

        MessagesListAdapter<ChatMessage>(viewModel?.userId, messageConfig, imageLoader).let { adapter ->

            adapter.enableSelectionMode(this)
            adapter.setLoadMoreListener(this)
            adapter.registerViewClickListener(R.id.messageUserAvatar) { view, message ->
                context?.let { Popup.showToast(it, message.text) }
            }

            messagesList?.setAdapter(adapter)
            messagesAdapter = adapter
        }
    }

    private fun setupChatInput(rootLayout: View) {

        val input = rootLayout.findViewById(R.id.input) as MessageInput
        input.setInputListener(this)
        input.setTypingListener(this)
        input.setAttachmentsListener(this)
    }

    private fun showMessages(messages: List<ChatMessage>) {

        messages.sortedBy { it.createdAt }.forEach {
            messagesAdapter?.addToStart(it, true)
        }
    }

//    fun startChat() {
//
//        val firstMessage = ChatMessage.new("anyMessageId", userId, "This Debug User", "debug chat message")
//        messagesAdapter?.addToStart(firstMessage, true)
//    }

    override fun onSelectionChanged(count: Int) {
        Log.i(TAG, "Debug:: onSelectionChanged(count: $count)")
    }

    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        Log.i(TAG, "Debug:: onLoadMore(page: $page, totalItemsCount: $totalItemsCount)")
    }

    override fun onSubmit(input: CharSequence?): Boolean {
        Log.i(TAG, "Debug:: onSubmit(input: $input)")
        val firstMessage = ChatMessage.new(DEFAULT_MESSAGE_ID, viewModel?.userId!!, viewModel?.userName!!, input.toString())
        messagesAdapter?.addToStart(firstMessage, true)
        return true
    }

    override fun onAddAttachments() {
        // not needed yet
    }

    override fun onStartTyping() {
        // not needed yet
    }

    override fun onStopTyping() {
        // not needed yet
    }
}