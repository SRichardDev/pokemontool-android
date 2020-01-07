package io.stanc.pogoradar.firebase.node

interface FirebaseListNode: FirebaseNode {

    fun list(): List<FirebaseNode>
}