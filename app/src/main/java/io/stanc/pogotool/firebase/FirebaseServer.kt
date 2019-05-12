package io.stanc.pogotool.firebase

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import io.stanc.pogotool.firebase.data.FirebaseData
import io.stanc.pogotool.firebase.node.FirebaseNode
import java.lang.ref.WeakReference


object FirebaseServer {
    private val TAG = javaClass.name

    // TODO?: use FirebaseFirestore instead of realtime FirebaseDatabase, but iOS uses FirebaseDatabase
    internal val database = FirebaseDatabase.getInstance().reference

    /**
     * callbacks
     */

    interface OnCompleteCallback<T> {
        fun onSuccess(data: T?)
        fun onFailed(message: String?)
    }

    private fun <TResult, TData>callback(task: Task<TResult>, callback: OnCompleteCallback<TData>) {

        if (task.isSuccessful) {

            (task.result as? InstanceIdResult)?.let { callback.onSuccess(it.token as TData) }
            (task.result as? Void)?.let { callback.onSuccess(null) }

        } else {
            callback.onFailed(task.exception?.message)
        }
    }

    private val nodeDidChangeListener = HashMap<Pair<Int, String>, NodeEventListener>()

    fun addNodeEventListener(databasePath: String, callback: OnNodeDidChangeCallback) {
        if (!alreadyAddedToList(databasePath, callback)) {

            val eventListener = NodeEventListener(callback)
            nodeDidChangeListener[Pair(callback.hashCode(), databasePath)] = eventListener
            database.child(databasePath).addValueEventListener(eventListener)
        }
    }

    fun removeNodeEventListener(databasePath: String, callback: OnNodeDidChangeCallback) {
        nodeDidChangeListener.remove(Pair(callback.hashCode(), databasePath))?.let {
            database.child(databasePath).removeEventListener(it)
        }
    }

    private fun alreadyAddedToList(databasePath: String, callback: OnNodeDidChangeCallback): Boolean {
        return nodeDidChangeListener.containsKey(Pair(callback.hashCode(), databasePath))
    }

    interface OnNodeDidChangeCallback {
        fun nodeChanged(dataSnapshot: DataSnapshot)
    }

    class NodeEventListener(callback: OnNodeDidChangeCallback): ValueEventListener {
        private val TAG = this.javaClass.name

        private val callback = WeakReference(callback)

        override fun onCancelled(p0: DatabaseError) {
            Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message})")
        }

        override fun onDataChange(p0: DataSnapshot) {
            Log.v(TAG, "onDataChange($p0), key: ${p0.key}, value: ${p0.value}, childs: ${p0.childrenCount}")
            callback.get()?.nodeChanged(p0)
        }
    }

    /**
     * interface for request, add, change & remove data
     */

    fun requestDataValue(databaseDataPath: String, onCompletionCallback: OnCompleteCallback<Any?>? = null) {
        FirebaseServer.database.child(databaseDataPath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for database path: $databaseDataPath")
                onCompletionCallback?.onFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.v(TAG, "onDataChange(key: ${p0.key}, value: ${p0.value}) for database path: $databaseDataPath")
                onCompletionCallback?.onSuccess(p0.value)
            }
        })
    }

    fun requestDataChilds(databaseChildPath: String, onCompletionCallback: OnCompleteCallback<List<DataSnapshot>>? = null) {
        FirebaseServer.database.child(databaseChildPath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for database path: $databaseChildPath")
                onCompletionCallback?.onFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.v(TAG, "onDataChange(key: ${p0.key}, value: ${p0.value}) for database path: $databaseChildPath")
                onCompletionCallback?.onSuccess(p0.children.toList())
            }
        })
    }

    // Hint: never use "setValue()" because this overwrites other child nodes!
    fun createNode(firebaseNode: FirebaseNode, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        database.child(firebaseNode.databasePath()).updateChildren(firebaseNode.data()).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun setData(firebaseData: FirebaseData, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        database.child(firebaseData.databasePath()).child(firebaseData.key).setValue(firebaseData.data()).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun createNodeByAutoId(databasePath: String, data: Map<String, Any>, onCompletionCallback: OnCompleteCallback<Void>? = null): String? {
        val newNode = database.child(databasePath).push()
        newNode.setValue(data).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
        return newNode.key
    }

    fun removeNode(firebaseNode: FirebaseNode, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        database.child(firebaseNode.databasePath()).child(firebaseNode.id).removeValue().addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun removeValue(firebaseData: FirebaseData, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        database.child(firebaseData.databasePath()).removeValue().addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }


    /**
     * helper
     */

    fun timestamp(): Any {
        return ServerValue.TIMESTAMP
    }

    fun requestNotificationToken(onCompletionCallback: OnCompleteCallback<String>) {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            callback<InstanceIdResult, String>(task, onCompletionCallback)
        }
    }
}