package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_RAID

class RaidMeetup(private val raidDatabasePath: String,
                 private val raidMeetupId: String): FirebaseData {

    override val key: String = "raidMeetupId"
    override fun databasePath(): String = "$raidDatabasePath/$DATABASE_RAID"
    override fun data(): String = raidMeetupId
}