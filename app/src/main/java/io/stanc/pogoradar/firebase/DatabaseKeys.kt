package io.stanc.pogoradar.firebase

import io.stanc.pogoradar.geohash.GeoHash

object DatabaseKeys {

    // Arenas
    const val ARENAS = "arenas"
//    const val ARENAS = "test_arenas"
    const val IS_EX = "isEX"

    // Pokestops
    const val POKESTOPS = "pokestops"
//    const val POKESTOPS = "test_pokestops"
    const val POKESTOP_QUEST_ID = "questId"

    // Raid
    const val RAID = "raid"
    const val RAID_ID = "raidId"
    const val RAID_LEVEL = "level"
    const val RAID_BOSS = "raidBoss"
    const val RAID_BOSS_DEFAULT = 0
    const val RAID_TIME_END = "endTime"
    const val RAID_TIME_EGG_HATCHES = "hatchTime"
    const val RAID_DURATION = 45

    // Raid Meetup
    const val RAID_MEETUP = "meetup"
    const val CHAT_ID = "chatId"
    const val MEETUP_TIME = "meetupTime"
    const val PARTICIPANTS = "participants"

    // Chat
    const val CHAT_MESSAGE = "message"
    const val CHAT_SENDER_ID = "senderId"

    // Raid Bosses
    const val RAID_BOSSES = "raidBosses"
    const val RAID_BOSS_IMAGE_NAME = "imageName"
    const val RAID_BOSS_LEVEL = "level"

    // Quests
    const val QUESTS = "quests"
    const val QUEST_IMAGE_NAME = "imageName"
    const val QUEST = "quest"
    const val QUEST_REWARD = "reward"
    const val QUEST_ID = "definitionId"

    // Users
    const val USERS = "users"
    const val EMAIL = "email"
    const val USER_ID = "id"
    const val NOTIFICATION_TOKEN = "notificationToken"
    const val USER_PLATFORM = "platform"
    const val USER_PLATFORM_ANDROID = "android"
    const val USER_PUBLIC_DATA = "publicData"
    const val USER_NAME = "trainerName"
    const val USER_LEVEL = "level"
    const val USER_TEAM = "team"
    const val USER_CODE = "trainerCode"
    const val USER_APP_LAST_OPENED = "appLastOpened"
    const val SUBMITTED_ARENAS = "submittedArenas"
    const val SUBMITTED_POKESTOPS = "submittedPokestops"
    const val SUBMITTED_QUESTS = "submittedQuests"
    const val SUBMITTED_RAIDS = "submittedRaids"
    const val SUBSCRIBED_RAID_MEETUPS = "subscribedRaidMeetups"
    const val SUBSCRIBED_GEOHASHES = "subscribedGeohashes"
    const val USER_TOPICS = "topics"

    // common
    const val REGISTERED_USERS = "registeredUsers"
    const val NAME = "name"
    const val SUBMITTER_ID = "submitterId"
    const val TIMESTAMP = "timestamp"
    const val TIMESTAMP_NONE: Long = 0
    const val GEO_HASH_AREA_PRECISION: Int = 6
    const val DATA_UNDEFINED = "---"
    const val DEFAULT_TIME = "--:--"

    // notifications
    const val NOTIFICATION_TYPE = "notificationType"
    const val NOTIFICATION_TYPE_RAID = "Raid Notifications"
    const val NOTIFICATION_TYPE_QUEST = "Quest Notifications"
    const val NOTIFICATION_TYPE_CHAT = "Chat Notifications"
    const val NOTIFICATION_TYPE_LOCAL = "Local Notifications"
    const val NOTIFICATION_TITLE = "title"
    const val NOTIFICATION_BODY = "body"
    const val NOTIFICATION_LATITUDE = "latitude"
    const val NOTIFICATION_LONGITUDE = "longitude"

    const val NOTIFICATION_TOPIC_ANDROID = "android"
    const val NOTIFICATION_TOPIC_LEVEL = "level-"
    const val NOTIFICATION_TOPIC_QUESTS = "quests"
    const val NOTIFICATION_TOPIC_RAIDS = "raids"
    const val NOTIFICATION_TOPIC_INCIDENTS = "incidents"

    const val MAX_SUBSCRIPTIONS = 100

    fun firebaseGeoHash(geoHash: GeoHash): String = geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)
}