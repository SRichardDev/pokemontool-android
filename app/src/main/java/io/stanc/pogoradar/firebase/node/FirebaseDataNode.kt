package io.stanc.pogoradar.firebase.node


interface FirebaseDataNode: FirebaseNode {

    fun data(): Map<String, Any>
}