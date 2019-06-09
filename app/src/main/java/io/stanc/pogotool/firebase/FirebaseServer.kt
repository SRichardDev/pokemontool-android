package io.stanc.pogotool.firebase

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import io.stanc.pogotool.firebase.node.FirebaseNode
import java.lang.ref.WeakReference


object FirebaseServer {
    private val TAG = javaClass.name

    private val connectionRef: DatabaseReference
    var connected: Boolean = false
        private set
    private val connectionChangeListener = HashMap<Int, (Boolean) -> Unit>()
    private val connectionEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            val connected = snapshot.getValue(Boolean::class.java) ?: false
            FirebaseServer.connected = connected
            for (listener in connectionChangeListener.values) {
                listener.invoke(connected)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "ConnectionEventListener: onCancelled(errorCode: ${error.code}, errorMsg: ${error.message})")
        }
    }

    init {
        // TODO: syncing data and offline handling
        // https@ //firebase.google.com/docs/databaseRef/android/offline-capabilities
        // https://stackoverflow.com/questions/40190234/firebase-what-is-the-difference-between-setpersistenceenabled-and-keepsynced
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        connectionRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectionRef.addValueEventListener(connectionEventListener)
    }

    // TODO?: use FirebaseFirestore instead of realtime FirebaseDatabase, but iOS uses FirebaseDatabase
    private val databaseRef = FirebaseDatabase.getInstance().reference


    /**
     * Connection
     */

    fun addConnectionListener(onConnectionDidChange: (connected: Boolean) -> Unit) {
        if (!connectionChangeListener.containsKey(onConnectionDidChange.hashCode())) {
            onConnectionDidChange.invoke(connected)
            connectionChangeListener[onConnectionDidChange.hashCode()] = onConnectionDidChange
        }
    }

    fun removeConnectionListener(onConnectionDidChange: (connected: Boolean) -> Unit) {
        connectionChangeListener.remove(onConnectionDidChange.hashCode())
    }


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
            databaseRef.child(databasePath).addValueEventListener(eventListener)
        }
    }

    fun removeNodeEventListener(databasePath: String, callback: OnNodeDidChangeCallback) {
        nodeDidChangeListener.remove(Pair(callback.hashCode(), databasePath))?.let {
            databaseRef.child(databasePath).removeEventListener(it)
        }
    }

    private fun alreadyAddedToList(databasePath: String, callback: OnNodeDidChangeCallback): Boolean {
        return nodeDidChangeListener.containsKey(Pair(callback.hashCode(), databasePath))
    }

    interface OnNodeDidChangeCallback {
        fun nodeChanged(dataSnapshot: DataSnapshot)
        fun nodeRemoved(key: String)
    }

    private class NodeEventListener(callback: OnNodeDidChangeCallback): ValueEventListener {
        private val TAG = this.javaClass.name

        private val callback = WeakReference(callback)

        override fun onCancelled(p0: DatabaseError) {
            Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message})")
        }

        override fun onDataChange(p0: DataSnapshot) {
//            Log.v(TAG, "onDataChange($p0), key: ${p0.key}, value: ${p0.value}, childs: ${p0.childrenCount}")
            if (p0.value == null) {
                p0.key?.let {
                    callback.get()?.nodeRemoved(it)
                } ?: kotlin.run {
                    Log.e(TAG, "onDataChange($p0) - value and key are null.")
                }
            } else {
                callback.get()?.nodeChanged(p0)
            }
        }
    }

    /**
     * interface for request, add, change & remove data
     */

    fun requestDataValue(databaseDataPath: String, onCompletionCallback: OnCompleteCallback<Any?>? = null) {
        FirebaseServer.databaseRef.child(databaseDataPath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for databaseRef path: $databaseDataPath")
                onCompletionCallback?.onFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
//                Log.v(TAG, "onDataChange(key: ${p0.key}, value: ${p0.value}) for databaseRef path: $databaseDataPath")
                onCompletionCallback?.onSuccess(p0.value)
            }
        })
    }

    fun requestDataChilds(databaseChildPath: String, onCompletionCallback: OnCompleteCallback<List<DataSnapshot>>? = null) {
        FirebaseServer.databaseRef.child(databaseChildPath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for databaseRef path: $databaseChildPath")
                onCompletionCallback?.onFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.v(TAG, "onDataChange(key: ${p0.key}, value: ${p0.value}) for databaseRef path: $databaseChildPath")
                onCompletionCallback?.onSuccess(p0.children.toList())
            }
        })
    }

    // Hint: never use "setValue()" because this overwrites other child nodes!
    fun updateNode(firebaseNode: FirebaseNode, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        databaseRef.child(firebaseNode.databasePath()).updateChildren(firebaseNode.data()).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun setNode(firebaseNode: FirebaseNode, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        databaseRef.child(firebaseNode.databasePath()).setValue(firebaseNode.data()).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun setData(databasePath: String, data: Any, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        databaseRef.child(databasePath).setValue(data).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun createNodeByAutoId(databasePath: String, data: Map<String, Any>, onCompletionCallback: OnCompleteCallback<Void>? = null): String? {
        val newNode = databaseRef.child(databasePath).push()
        newNode.setValue(data).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
        return newNode.key
    }

    fun setDataByAutoId(databasePath: String, data: Any, onCompletionCallback: OnCompleteCallback<Void>? = null): String? {
        val newNode = databaseRef.child(databasePath).push()
        newNode.setValue(data).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
        return newNode.key
    }

    fun setDataKey(databasePath: String, key: String, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        val data = HashMap<String, String>()
        data[key] = ""
        databaseRef.child(databasePath).setValue(data).addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

    fun removeNode(firebaseNode: FirebaseNode, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        databaseRef.child(firebaseNode.databasePath()).child(firebaseNode.id).removeValue().addOnCompleteListener { task ->
            onCompletionCallback?.let { callback<Void, Void>(task, it) }
        }
    }

//    fun removeData(firebaseData: FirebaseData, onCompletionCallback: OnCompleteCallback<Void>? = null) {
//        databaseRef.child(firebaseData.databasePath()).removeData().addOnCompleteListener { task ->
//            onCompletionCallback?.let { callback<Void, Void>(task, it) }
//        }
//    }

    fun removeData(databasePath: String, onCompletionCallback: OnCompleteCallback<Void>? = null) {
        databaseRef.child(databasePath).removeValue().addOnCompleteListener { task ->
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