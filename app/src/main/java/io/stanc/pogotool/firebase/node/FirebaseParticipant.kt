package io.stanc.pogotool.firebase.node

data class FirebaseParticipant private constructor(
    override val id: String,
    val name: String): FirebaseNode {

    override fun databasePath(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun data(): Map<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
