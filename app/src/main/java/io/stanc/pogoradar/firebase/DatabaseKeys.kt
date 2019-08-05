package io.stanc.pogoradar.firebase

import io.stanc.pogoradar.geohash.GeoHash

object DatabaseKeys {

    // Arenas
    const val ARENAS = "arenas"
//    const val ARENAS = "test_arenas"
    const val IS_EX = "isEX"
    const val RAID = "raid"
    const val RAID_LEVEL = "level"
    const val RAID_BOSS_ID = "raidBossId"
    const val RAID_MEETUP_ID = "raidMeetupId"
    const val RAID_TIME_END = "endTime"
    const val RAID_TIME_EGG_HATCHES = "hatchTime"
    const val RAID_DURATION = 45

    // Pokestops
    const val POKESTOPS = "pokestops"
//    const val POKESTOPS = "test_pokestops"
    const val POKESTOP_QUEST_ID = "questId"

    // Raid Meetup
    const val RAID_MEETUPS = "raidMeetups"
    const val CHAT = "chats"
    const val CHAT_MESSAGE = "message"
    const val CHAT_SENDER_ID = "senderId"
    const val MEETUP_TIME = "meetupTime"
    const val DEFAULT_MEETUP_TIME = "--:--"
    const val PARTICIPANTS = "participants"

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
    const val NOTIFICATION_ACTIVE = "isPushActive"
    const val USER_PLATFORM = "platform"
    const val USER_PUBLIC_DATA = "publicData"
    const val USER_NAME = "trainerName"
    const val USER_LEVEL = "level"
    const val USER_TEAM = "team"
    const val USER_CODE = "trainerCode"
    const val SUBMITTED_ARENAS = "submittedArenas"
    const val SUBMITTED_POKESTOPS = "submittedPokestops"
    const val SUBMITTED_QUESTS = "submittedQuests"
    const val SUBMITTED_RAIDS = "submittedRaids"
    const val SUBSCRIBED_GEOHASH_POKESTOPS = "subscribedGeohashPokestops"
    const val SUBSCRIBED_GEOHASH_ARENAS = "subscribedGeohashArenas"

    // common
    const val REGISTERED_USERS = "registeredUsers"
    const val REGISTERED_USERS_ARENAS = "registeredUsersArenas"
    const val REGISTERED_USERS_POKESTOPS = "registeredUsersPokestops"
    const val NAME = "name"
    const val SUBMITTER = "submitter"
    const val TIMESTAMP = "timestamp"
    const val GEO_HASH_AREA_PRECISION: Int = 6
    const val DATA_UNDEFINED = "---"

    // notifications
    const val NOTIFICATION_TYPE = "notificationType"
    const val NOTIFICATION_TYPE_RAID = "Raid Notifications"
    const val NOTIFICATION_TYPE_QUEST = "Quest Notifications"
    const val NOTIFICATION_TYPE_CHAT = "Chat Notifications"
    const val NOTIFICATION_TITLE = "title"
    const val NOTIFICATION_BODY = "body"
    const val NOTIFICATION_LATITUDE = "latitude"
    const val NOTIFICATION_LONGITUDE = "longitude"

    const val NOTIFICATION_TOPIC_LEVEL = "level-"
    const val NOTIFICATION_TOPIC_QUESTS = "quests"
    const val NOTIFICATION_TOPIC_RAIDS = "raids"
    const val NOTIFICATION_TOPIC_MEETUPS_GROUP = "meetups"
    const val NOTIFICATION_TOPIC_INCIDENT = "incident"

    const val MAX_SUBSCRIPTIONS = 100
    const val PLATFORM_ANDROID = "android"

    fun firebaseGeoHash(geoHash: GeoHash): String = geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)

    enum class SubscriptionType(val userDataKey: String, val subscriptionDatabaseKey: String) {
        Arena(SUBSCRIBED_GEOHASH_ARENAS, REGISTERED_USERS_ARENAS),
        Pokestop(SUBSCRIBED_GEOHASH_POKESTOPS, REGISTERED_USERS_POKESTOPS)
    }
}