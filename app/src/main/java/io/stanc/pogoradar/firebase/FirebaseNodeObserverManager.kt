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
        nodeObserverManager.addObserver(observer, subId = node.id)
        FirebaseServer.addNodeEventListener(node, arenaDidChangeCallback)
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebaseNodeType>, node: FirebaseNode) {
        FirebaseServer.removeNodeEventListener(node, arenaDidChangeCallback)
        nodeObserverManager.removeObserver(observer, subId = node.id)
    }
}