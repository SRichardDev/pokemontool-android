package io.stanc.pogoradar.firebase.data

import io.stanc.pogoradar.firebase.DatabaseKeys.RAID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUP_ID

class RaidMeetup(private val raidDatabasePath: String,
                 private val raidMeetupId: String): FirebaseData {

    override val key: String = RAID_MEETUP_ID
    override fun databasePath(): String = "$raidDatabasePath/$RAID"
    override fun data(): String = raidMeetupId
}