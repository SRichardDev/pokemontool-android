package io.stanc.pogoradar.firebase

import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.node.FirebaseNode
import io.stanc.pogoradar.utils.ObserverManager


class FirebaseNodeObserverManager<FirebaseNodeType: FirebaseNode>(private val newFirebaseNode:(dataSnapshot: DataSnapshot) -> FirebaseNodeType?) {

    private val nodeObserverManager = ObserverManager<FirebaseNodeObserver<FirebaseNodeType>>()

    private val arenaDidChangeCallback = object : FirebaseServer.OnNodeDidChangeCallback {
        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            newFirebaseNode(dataSnapshot)?.let { node ->
                nodeObserverManager.observers(node.id).filterNotNull().forEach { it.onItemChanged(node) }
            }
        }

        override fun nodeRemoved(key: String) {
            nodeObserverManager.observers(key).filterNotNull().forEach { it.onItemRemoved(key) }
        }
    }

    fun observers(nodeId: String): List<FirebaseNodeObserver<FirebaseNodeType>?> = nodeObserverManager.observers(subId = nodeId)

    fun clear() = nodeObserverManager.clear()

    fun addObserver(observer: FirebaseNodeObserver<FirebaseNodeType>, node: FirebaseNode) {
        addObserver(observer, node.databasePath(), node.id)
    }

    fun addObserver(observer: FirebaseNodeObserver<FirebaseNodeType>, databasePath: String, nodeId: String) {
        FirebaseServer.addNodeEventListener("$databasePath/$nodeId", arenaDidChangeCallback)
        nodeObserverManager.addObserver(observer, subId = nodeId)
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebaseNodeType>, node: FirebaseNode) {
        removeObserver(observer, node.databasePath(), node.id)
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebaseNodeType>, databasePath: String, nodeId: String) {
        FirebaseServer.removeNodeEventListener("$databasePath/$nodeId", arenaDidChangeCallback)
        nodeObserverManager.removeObserver(observer, subId = nodeId)
    }
}