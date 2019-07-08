package io.stanc.pogoradar.firebase

import io.stanc.pogoradar.utils.Kotlin

data class NotificationContent( val title: String,
                                val body: String,
                                val latitude: Double,
                                val longitude: Double) {

    companion object {

        fun new(title: String?,
                body: String?,
                latitude: Double?,
                longitude: Double?): NotificationContent? {

            return Kotlin.safeLet(title, body, latitude, longitude) { title, body, latitude, longitude ->
                NotificationContent(title, body, latitude, longitude)
            } ?: run {
                null
            }
        }
    }
}

