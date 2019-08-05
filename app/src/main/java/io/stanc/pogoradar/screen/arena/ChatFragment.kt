package io.stanc.pogoradar.screen.arena

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import co.chatsdk.core.error.ChatSDKException
import co.chatsdk.firebase.file_storage.FirebaseFileStorageModule.activate
import co.chatsdk.firebase.file_storage.FirebaseFileStorageModule
import co.chatsdk.ui.manager.BaseInterfaceAdapter
import co.chatsdk.firebase.FirebaseNetworkAdapter
import co.chatsdk.core.session.ChatSDK
import co.chatsdk.core.session.Configuration
import io.stanc.pogoradar.R


class ChatFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_chat, container, false)
        return rootLayout
    }

    fun startChat() {

        ChatSDK.ui().startChatActivityForID()

    }
}