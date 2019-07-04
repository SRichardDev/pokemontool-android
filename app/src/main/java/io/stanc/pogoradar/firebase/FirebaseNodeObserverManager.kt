package io.stanc.pogoradar.firebase

import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.node.FirebaseNode
import io.stanc.pogoradar.utils.ObserverManager

class FirebaseNodeObserverManager<FirebaseNodeType: FirebaseNode>(private val newFirebaseNode:(dataSnapshot: DataSnapshot) -> FirebaseNodeType?) {

    private val nodeObserverManager = ObserverManager<Observer<FirebaseNodeType>>()

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

    interface Observer<FirebaseNodeType> {
        fun onItemChanged(item: FirebaseNodeType)
        fun onItemRemoved(itemId: String)
    }

    fun addObserver(observer: Observer<FirebaseNodeType>, node: FirebaseNode) {
        FirebaseServer.addNodeEventListener("${node.databasePath()}/${node.id}", arenaDidChangeCallback)
        nodeObserverManager.addObserver(observer, subId = node.id)
    }

    fun removeObserver(observer: Observer<FirebaseNodeType>, node: FirebaseNode) {
        FirebaseServer.removeNodeEventListener("${node.databasePath()}/${node.id}", arenaDidChangeCallback)
        nodeObserverManager.removeObserver(observer, subId = node.id)
    }
}