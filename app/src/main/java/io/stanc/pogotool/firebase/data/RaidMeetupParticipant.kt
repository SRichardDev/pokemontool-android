package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogotool.firebase.DatabaseKeys.PARTICIPANTS

data class RaidMeetupParticipant(private val raidMeetupId: String,
                                 override val key: String,
                                 private val userId: String): FirebaseData {

    override fun databasePath(): String = "$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS"
    override fun data(): String = userId
}