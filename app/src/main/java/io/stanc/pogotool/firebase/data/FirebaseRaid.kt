package io.stanc.pogotool.firebase.data

data class FirebaseRaid(override val id: String): FirebaseNode {

    override fun databasePath(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun data(): Map<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    Raid(level: selectedRaidLevel Int,
//    raidBoss: selectedRaidBoss,
//    timeLeft: selectedTimeLeft,
//    raidMeetupId: id)
}