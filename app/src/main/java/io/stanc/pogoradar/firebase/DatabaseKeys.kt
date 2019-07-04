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
    const val CHAT = "chat"
    const val CHAT_MESSAGE = "chat"
    const val CHAT_SENDER_ID = "chat"
    const val MEETUP_TIME = "meetupTime"
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
    const val USER_PUBLIC_DATA = "publicData"
    const val USER_NAME = "trainerName"
    const val USER_LEVEL = "level"
    const val USER_TEAM = "team"
    const val USER_CODE = "trainerCode"
    const val SUBMITTED_ARENAS = "submittedArenas"
    const val SUBMITTED_POKESTOPS = "submittedPokestops"
    const val SUBMITTED_QUESTS = "submittedQuests"
    const val SUBMITTED_RAIDS = "submittedRaids"

    // common
    const val REGISTERED_USERS = "registeredUsers"
    const val NAME = "name"
    const val SUBMITTER = "submitter"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val TIMESTAMP = "timestamp"
    const val GEO_HASH_AREA_PRECISION: Int = 6

    const val MAX_SUBSCRIPTIONS = 16

    fun firebaseGeoHash(geoHash: GeoHash): String = geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)

}