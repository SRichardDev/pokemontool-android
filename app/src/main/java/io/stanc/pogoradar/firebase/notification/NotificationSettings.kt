package io.stanc.pogoradar.firebase.notification

import androidx.databinding.ObservableField
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOPIC_ANDROID
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOPIC_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOPIC_QUESTS
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOPIC_RAIDS
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.utils.addOnPropertyChanged

object NotificationSettings {

    private val TAG = javaClass.name

    val enableNotifications = ObservableField<Boolean>(true)

    val enableNotificationsForQuests = ObservableField<Boolean>(true)
    // TODO: NOTIFICATION_TOPIC_INCIDENTS
    val enableNotificationsForRaids = ObservableField<Boolean>(true)
    val enableNotificationsForRaidsWith1Star = ObservableField<Boolean>(true)
    val enableNotificationsForRaidsWith2Star = ObservableField<Boolean>(true)
    val enableNotificationsForRaidsWith3Star = ObservableField<Boolean>(true)
    val enableNotificationsForRaidsWith4Star = ObservableField<Boolean>(true)
    val enableNotificationsForRaidsWith5Star = ObservableField<Boolean>(true)

    private val userDataObserver = object: FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {

            user?.isNotificationActive?.let {
                enableNotifications.set(it)
            }

            user?.notificationTopics?.let { topics ->

                enableNotificationsForQuests.set(topics.contains(NOTIFICATION_TOPIC_QUESTS))
                enableNotificationsForRaids.set(topics.contains(NOTIFICATION_TOPIC_RAIDS))
                enableNotificationsForRaidsWith1Star.set(topics.contains("${NOTIFICATION_TOPIC_LEVEL}1"))
                enableNotificationsForRaidsWith2Star.set(topics.contains("${NOTIFICATION_TOPIC_LEVEL}2"))
                enableNotificationsForRaidsWith3Star.set(topics.contains("${NOTIFICATION_TOPIC_LEVEL}3"))
                enableNotificationsForRaidsWith4Star.set(topics.contains("${NOTIFICATION_TOPIC_LEVEL}4"))
                enableNotificationsForRaidsWith5Star.set(topics.contains("${NOTIFICATION_TOPIC_LEVEL}5"))
            }
        }
    }

    init {
        setupOnPropertiesChanged()
        FirebaseUser.addUserDataObserver(userDataObserver)
    }

    private fun setupOnPropertiesChanged() {

        addOnPropertyChangedForTopicNotification(
            enableNotifications,
            NOTIFICATION_TOPIC_ANDROID
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForQuests,
            NOTIFICATION_TOPIC_QUESTS
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForRaids,
            NOTIFICATION_TOPIC_RAIDS
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForRaidsWith1Star,
            "${NOTIFICATION_TOPIC_LEVEL}1"
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForRaidsWith2Star,
            "${NOTIFICATION_TOPIC_LEVEL}2"
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForRaidsWith3Star,
            "${NOTIFICATION_TOPIC_LEVEL}3"
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForRaidsWith4Star,
            "${NOTIFICATION_TOPIC_LEVEL}4"
        )
        addOnPropertyChangedForTopicNotification(
            enableNotificationsForRaidsWith5Star,
            "${NOTIFICATION_TOPIC_LEVEL}5"
        )
    }

    private fun addOnPropertyChangedForTopicNotification(observableField: ObservableField<Boolean>, notificationTopic: String) {

        observableField.addOnPropertyChanged { enableNotifications ->

            if (enableNotifications.get() == true) {
                FirebaseNotification.subscribeToTopic(notificationTopic)
            } else {
                FirebaseNotification.unsubscribeFromTopic(notificationTopic)
            }
        }
    }
}