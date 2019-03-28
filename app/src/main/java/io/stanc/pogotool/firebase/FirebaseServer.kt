package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import io.stanc.pogotool.R
import io.stanc.pogotool.utils.WaitingSpinner
import java.lang.ref.WeakReference
import io.stanc.pogotool.firebase.data.*


object FirebaseServer {

    private val TAG = javaClass.name

    //    TODO?: use FirebaseFirestore instead of realtime FirebaseDatabase, but iOS uses FirebaseDatabase
//    private val firebase = FirebaseFirestore.getInstance()

    internal val database = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var currentUser: io.stanc.pogotool.firebase.data.FirebaseUser? = null
        private set

    fun start() {
        auth.addAuthStateListener { onAuthStateChanged() }
    }

    fun stop() {
        auth.removeAuthStateListener { onAuthStateChanged }
    }

    /**
     * authentication listener
     */

    private val authStateObservers = HashMap<Int, WeakReference<AuthStateObserver>>()

    enum class AuthState {
        UserLoggedOut,
        UserLoggedInButUnverified,
        UserLoggedIn
    }
    interface AuthStateObserver {
        fun authStateChanged(newAuthState: AuthState)
    }

//  TODO: see https://firebase.google.com/docs/auth/web/manage-users
    fun addAuthStateObserver(observer: AuthStateObserver) {
        val weakObserver = WeakReference(observer)
        authStateObservers[observer.hashCode()] = weakObserver
    }

    fun removeAuthStateObserver(observer: AuthStateObserver) {
        authStateObservers.remove(observer.hashCode())
    }

    private val onAuthStateChanged: () -> Unit = {

        updateUserProfile(auth.currentUser)

        val authState = authState()
        Log.i(TAG, "onAuthStateChanged(${authState.name}) for user: $currentUser")
        authStateObservers.forEach { it.value.get()?.authStateChanged(authState) }

        when(authState) {
            AuthState.UserLoggedIn, AuthState.UserLoggedInButUnverified -> {
                updateUsersNotificationToken()
            }
            else -> { /* nothing */ }
        }
    }

    private fun updateUsersNotificationToken() {

        requestNotificationToken(onRequestResponds = { token ->

            val data = HashMap<String, String>()
            data[io.stanc.pogotool.firebase.FirebaseDatabase.DATABASE_NOTIFICATION_TOKEN] = token
            sendUserData(data)

            currentUser?.notificationToken = token
        })
    }

    fun authState(): AuthState {

        auth.currentUser?.let { firebaseUser ->

            return if (firebaseUser.isEmailVerified) {
                AuthState.UserLoggedIn
            } else {
                AuthState.UserLoggedInButUnverified
            }

        } ?: kotlin.run {
            return AuthState.UserLoggedOut
        }
    }

    fun authStateText(context: Context): String {

        return when (authState()) {

            AuthState.UserLoggedIn -> context.getString(R.string.authentication_state_signed_in)
            AuthState.UserLoggedInButUnverified -> context.getString(R.string.authentication_state_signed_in_but_missing_verification)
            AuthState.UserLoggedOut -> context.getString(R.string.authentication_state_signed_out)
        }
    }

    /**
     * user profile listener
     */

    private val userProfileObservers = HashMap<Int, WeakReference<UserProfileObserver>>()

    interface UserProfileObserver {
        fun userProfileChanged(user: io.stanc.pogotool.firebase.data.FirebaseUser?)
    }

    //  TODO: see https://firebase.google.com/docs/auth/web/manage-users
    fun addUserProfileObserver(observer: UserProfileObserver) {
        val weakObserver = WeakReference(observer)
        userProfileObservers[observer.hashCode()] = weakObserver
    }

    fun removeUserProfileObserver(observer: UserProfileObserver) {

        val checkCount = userProfileObservers.count()
        userProfileObservers.remove(observer.hashCode())
        val newCheckCount = userProfileObservers.count()
        if (newCheckCount >= checkCount) {
            Log.e(TAG, "could not remove observer: $observer from list: checkCount: $checkCount, newCheckCount: $newCheckCount")
        }
    }

    /**
     * authentication methods
     */

    fun signUp(email: String, password: String, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = {_, _ ->}) {

        if (email.isEmpty() || password.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {task ->

            if (!task.isSuccessful) {
                Log.w(TAG, "signing up failed with error: ${task.exception?.message}")
            }

            onCompletionCallback(task.isSuccessful, task.exception?.message)
            WaitingSpinner.hideProgress()
        }
    }

    fun signIn(email: String, password: String, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = {_, _ ->}) {

        if (email.isEmpty() || password.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress()

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

            if (!task.isSuccessful) {
                Log.w(TAG, "signing in failed with error: ${task.exception?.message}")
            }

            onCompletionCallback(task.isSuccessful, task.exception?.message)
            WaitingSpinner.hideProgress()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendEmailVerification(onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = {_, _ ->}) {

        // Send verification email
        auth.currentUser?.let { user ->

            WaitingSpinner.showProgress()

            user.sendEmailVerification().addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.w(TAG, "email verification failed with error: ${task.exception?.message}")
                }

                WaitingSpinner.hideProgress()
                onCompletionCallback(task.isSuccessful, task.exception?.message)
            }
        } ?: kotlin.run { Log.w(TAG, "can not send email verification because current currentUser is null!") }
    }

    fun deleteUser() {
        // TODO...
        // auth.currentUser?.delete()
        // updateUserProfile()
    }

    /**
     * user
     */

    fun changeUserName(newUserName: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUserName)
            .setPhotoUri(currentUser?.photoURL)
            .build()

        auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i(TAG, "User profile updated.")
                updateUserProfile(auth.currentUser)
                onCompletionCallback(true)
            } else {

                Log.w(TAG, "User profile update failed. Error: ${task.exception?.message}")
                onCompletionCallback(false)
            }
        }
    }

    fun reloadUserData(context: Context, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        auth.currentUser?.reload()?.addOnCompleteListener { task ->

            if (task.isSuccessful) {
                updateUserProfile(auth.currentUser)
            } else {
                Log.w(TAG, "reloading currentUser data failed with error: ${task.exception?.message}")
                Toast.makeText(context, context.getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_LONG).show()
            }

            onCompletionCallback(task.isSuccessful)
        }
    }

    private fun updateUserProfile(firebaseAuthUser: FirebaseUser?) {

        firebaseAuthUser?.let { authUser ->

            currentUser?.let { currentUser ->

                currentUser.id = authUser.uid
                currentUser.name = authUser.displayName
                currentUser.email = authUser.email
                currentUser.isVerified = authUser.isEmailVerified
                currentUser.photoURL = authUser.photoUrl

            } ?: kotlin.run {

                currentUser = FirebaseUser(
                    authUser.uid,
                    authUser.displayName,
                    authUser.email,
                    authUser.isEmailVerified,
                    null,
                    authUser.photoUrl
                )
            }

            userProfileObservers.forEach { observer -> observer.value.get()?.userProfileChanged(currentUser) }

        } ?: kotlin.run {
            userProfileObservers.forEach { it.value.get()?.userProfileChanged(null) }
        }
    }

    /**
     * interface for request, add, change & remove data
     */

    fun requestData(databaseChildPath: String, onRequestResponds: (data: Any) -> Unit) {

        FirebaseServer.database.child(databaseChildPath).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for data: $databaseChildPath")
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.d(TAG, "onDataChange(data: ${p0.value}) for data: $databaseChildPath")
                p0.value?.let { onRequestResponds(it) }
            }
        })
    }

    // Hint: never use "setValue()" because this overwrites other child nodes!
    fun addDataToNode(firebaseNode: FirebaseNode, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        database.child(firebaseNode.databasePath()).updateChildren(firebaseNode.data()).addOnCompleteListener { task ->
            onCompletionCallback(task.isSuccessful)
        }
    }

    fun addNewNode(databasePath: String, data: Map<String, Any>, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        database.child(databasePath).push().setValue(data).addOnCompleteListener { onCompletionCallback(it.isSuccessful) }
    }

    fun removeData(firebaseNode: FirebaseNode, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        database.child(firebaseNode.databasePath()).child(firebaseNode.id).removeValue().addOnCompleteListener { onCompletionCallback(it.isSuccessful) }
    }

    /**
     * private implementation
     */

    private fun requestNotificationToken(onRequestResponds: (token: String) -> Unit) {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener { task ->

            if (task.isSuccessful) {
                task.result?.token?.let { token ->
                    onRequestResponds(token)
                }

            } else {
                Log.e(TAG, "requestNotificationToken failed: ", task.exception)
                return@OnCompleteListener
            }
        })
    }

    private fun sendUserData(data: Map<String, String>) {
        currentUser?.let {
            FirebaseServer.addDataToNode(it)
        } ?: kotlin.run { Log.w(TAG, "update user data failed for data: $data, currentUser: $currentUser") }
    }
}