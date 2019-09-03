package io.stanc.pogoradar.firebase

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.google.firebase.messaging.FirebaseMessaging
import io.stanc.pogoradar.firebase.node.FirebaseNode
import io.stanc.pogoradar.utils.Async
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


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

    // TODO: remove OnCompleteCallback and use another way to handle multiple methods with same callback structure
    private fun <TResult, TData>callback(task: Task<TResult>, callback: OnCompleteCallback<TData>) {

        if (task.isSuccessful) {

            (task.result as? InstanceIdResult)?.let { callback.onSuccess(it.token as? TData) }
            (task.result as? Void)?.let { callback.onSuccess(null) }

            // e.g. if no value exists for removing
            if (task.result == null) {
                callback.onSuccess(null)
            }

        } else {
            callback.onFailed(task.exception?.message)
        }
    }

    private suspend fun <T> awaitTaskCompletion(block: (OnCompleteListener<T>) -> Unit) : Boolean =
        suspendCancellableCoroutine { cont ->
            block(OnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.e(TAG, "awaitCompletion, task failed with exception: ${task.exception?.message}")
                }

                cont.resume(task.isSuccessful)
            })
        }

    /**
     * Node listener
     */

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
                } ?: run {
                    Log.e(TAG, "onDataChange($p0) - value and key are null.")
                }
            } else {
                callback.get()?.nodeChanged(p0)
            }
        }
    }

    /**
     * Child list listener
     */

    private val listDidChangeListener = HashMap<Pair<Int, String>, ListEventListener>()

    fun addListEventListener(databasePath: String, callback: OnChildDidChangeCallback) {
        if (!alreadyAddedToList(databasePath, callback)) {

            val eventListener = ListEventListener(callback)
            listDidChangeListener[Pair(callback.hashCode(), databasePath)] = eventListener
            databaseRef.child(databasePath).addChildEventListener(eventListener)
        }
    }

    fun removeListEventListener(databasePath: String, callback: OnChildDidChangeCallback) {
        listDidChangeListener.remove(Pair(callback.hashCode(), databasePath))?.let {
            databaseRef.child(databasePath).removeEventListener(it)
        }
    }

    private fun alreadyAddedToList(databasePath: String, callback: OnChildDidChangeCallback): Boolean {
        return listDidChangeListener.containsKey(Pair(callback.hashCode(), databasePath))
    }

    interface OnChildDidChangeCallback {
        fun childAdded(dataSnapshot: DataSnapshot)
        fun childChanged(dataSnapshot: DataSnapshot)
        fun childRemoved(kdataSnapshot: DataSnapshot)
    }

    private class ListEventListener(callback: OnChildDidChangeCallback): ChildEventListener {
        private val TAG = this.javaClass.name

        private val callback = WeakReference(callback)

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "onCancelled(error: $error), listener was removed for ${callback.get()}")
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, childName: String?) {
            callback.get()?.childChanged(dataSnapshot)
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, childName: String?) {
            callback.get()?.childChanged(dataSnapshot)
        }

        override fun onChildAdded(dataSnapshot: DataSnapshot, childName: String?) {
            callback.get()?.childAdded(dataSnapshot)
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            callback.get()?.childRemoved(dataSnapshot)
        }
    }

    /**
     * interface for request, add, change & remove data
     */

    fun requestNode(databaseNodePath: String, onCompletionCallback: OnCompleteCallback<DataSnapshot>? = null) {
        FirebaseServer.databaseRef.child(databaseNodePath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for databaseRef path: $databaseNodePath")
                onCompletionCallback?.onFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.v(TAG, "onDataChange(p0: $p0) for databaseRef path: $databaseNodePath")
                onCompletionCallback?.onSuccess(p0)
            }
        })
    }

    fun requestDataValue(databaseDataPath: String, onCompletionCallback: OnCompleteCallback<Any?>? = null) {
        FirebaseServer.databaseRef.child(databaseDataPath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for databaseRef path: $databaseDataPath")
                onCompletionCallback?.onFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                onCompletionCallback?.onSuccess(p0.value)
            }
        })
    }

    fun keepSyncDatabaseChilds(databasePath: String, keepSynced: Boolean = true) {
        FirebaseServer.databaseRef.child(databasePath).ref.keepSynced(keepSynced)
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
    fun updateNode(firebaseNode: FirebaseNode, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(firebaseNode.databasePath()).updateChildren(firebaseNode.data()).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "updateNode '$firebaseNode' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun setNode(firebaseNode: FirebaseNode, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(firebaseNode.databasePath()).setValue(firebaseNode.data()).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "setNode '$firebaseNode' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun setData(databasePath: String, data: Any, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(databasePath).setValue(data).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "setData '$data' in $databasePath failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun createNodeByAutoId(databasePath: String, data: Map<String, Any>, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}): String? {
        val newNode = databaseRef.child(databasePath).push()
        newNode.setValue(data).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "createNodeByAutoId '$databasePath' with data: $data failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
        return newNode.key
    }

    fun setDataByAutoId(databasePath: String, data: Any, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}): String? {
        val newNode = databaseRef.child(databasePath).push()
        newNode.setValue(data).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "setDataByAutoId data: $data in '$databasePath' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
        return newNode.key
    }

    fun addData(databasePath: String, data: HashMap<String, Any>, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(databasePath).updateChildren(data).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "addData data: $data in '$databasePath' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun addData(databasePath: String, key: String, value: Any, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(databasePath).child(key).setValue(value).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "addData [key: $key, value: $value] in '$databasePath' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun removeNode(firebaseNode: FirebaseNode, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        removeNode(firebaseNode.databasePath(), firebaseNode.id, onCompletionCallback)
    }

    fun removeNode(noteDatabasePath: String, nodeId: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(noteDatabasePath).child(nodeId).removeValue().addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "removeNode '$noteDatabasePath' with id:$nodeId failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun removeData(databasePath: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        databaseRef.child(databasePath).removeValue().addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "removeData '$databasePath' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun subscribeToTopic(topic: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "subscribeToTopic '$topic' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun unsubscribeFromTopic(topic: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener { task ->
            task.exception?.let {
                Log.e(TAG, "unsubscribeFromTopic '$topic' failed. Exception: [$it]")
            }
            onCompletionCallback(task.isSuccessful)
        }
    }

    suspend fun unsubscribeFromTopic(topic: String): Boolean {

        val successful = awaitTaskCompletion<Void> {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener(it)
        }

        return successful
    }


    /**
     * helper
     */

    const val TIMESTAMP_SERVER: Long = -1

    fun timestamp(): Any {
        return ServerValue.TIMESTAMP
    }

    fun requestNotificationToken(onCompletionCallback: OnCompleteCallback<String>) {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            callback<InstanceIdResult, String>(task, onCompletionCallback)
        }
    }
}