package io.stanc.pogoradar.chat

import com.stfalcon.chatkit.commons.models.IUser

class ChatUser private constructor(
    private val id: String,
    private val name: String,
    private val imageUrl: String? = null): IUser {

    override fun getAvatar(): String? = imageUrl
    override fun getName(): String = name
    override fun getId(): String = id

    companion object {

        fun new(id: String, name: String): ChatUser {
            return ChatUser(id, name)
        }
    }
}