package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_ARENA_RAID_MEETUPS

data class RaidMeetupParticipant(private val raidMeetupId: String,
                                 override val key: String,
                                 private val userId: String): FirebaseData {

    override fun databasePath(): String = "$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId/participants"
    override fun data(): String = userId
}