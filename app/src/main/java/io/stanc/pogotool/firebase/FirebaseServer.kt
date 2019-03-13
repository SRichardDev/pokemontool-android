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

    internal val database = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var currentUser: FirebaseUserLocal = FirebaseUserLocal()
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

        val checkCount = authStateObservers.count()
        authStateObservers.remove(observer.hashCode())
        val newCheckCount = authStateObservers.count()
        if (newCheckCount >= checkCount) {
            Log.e(TAG, "could not remove observer: $observer from list: checkCount: $checkCount, newCheckCount: $newCheckCount")
        }
    }

    private val onAuthStateChanged: () -> Unit = {

        updateUserProfile(auth.currentUser)

        val authState = authState()
        Log.i(TAG, "onAuthStateChanged(${authState.name}) for user: $currentUser")
        authStateObservers.forEach { it.value.get()?.authStateChanged(authState) }

        when(authState) {
            AuthState.UserLoggedIn, AuthState.UserLoggedInButUnverified -> {

                auth.currentUser?.let { updateNotificationToken(it) }
                    ?: kotlin.run { Log.e(TAG, "Wrong state. currentUser: ${auth.currentUser}, but authState: $authState") }
            }
            else -> { /* nothing */ }
        }
    }

    private fun updateNotificationToken(firebaseUser: FirebaseUser) {

        registerForNotificationToken(firebaseUser, onDataChanged = { notificationToken ->
            Log.i(TAG, "Debug:: currentUser: $currentUser, registerForNotificationToken.onDataChanged, token: $notificationToken")
            currentUser.notificationToken = notificationToken
            Log.i(TAG, "Debug:: currentUser: $currentUser")
        })

        sendTokenRequest(onRequestResponds = { token ->
            Log.i(TAG, "Debug:: sendTokenRequest.onRequestResponds, token: $token")
            val data = HashMap<String, String>()
            data[DATABASE_NOTIFICATION_TOKEN] = token
            sendUserData(data)
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

    private fun sendTokenRequest(onRequestResponds: (token: String) -> Unit) {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(OnCompleteListener { task ->

                if (task.isSuccessful) {
                    task.result?.token?.let { token ->
                        onRequestResponds(token)
                    }

                } else {
                    Log.e(TAG, "FirebaseInstanceId failed: ", task.exception)
                    return@OnCompleteListener
                }
            })
    }

    /**
     * user profile listener
     */

    private val userProfileObservers = HashMap<Int, WeakReference<UserProfileObserver>>()

    interface UserProfileObserver {
        fun userProfileChanged(user: FirebaseUserLocal)
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

    fun signUp(email: String, password: String, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        if (email.isEmpty() || password.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {task ->

            if (!task.isSuccessful) {
                Log.w(TAG, "signing up failed with error: ${task.exception?.message}")
            }

            onCompletedCallback(task.isSuccessful)
            WaitingSpinner.hideProgress()
        }
    }

    fun signIn(email: String, password: String, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        if (email.isEmpty() || password.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress()

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

            if (!task.isSuccessful) {
                Log.w(TAG, "signing in failed with error: ${task.exception?.message}")
            }

            onCompletedCallback(task.isSuccessful)
            WaitingSpinner.hideProgress()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendEmailVerification(onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        // Send verification email
        auth.currentUser?.let { user ->

            WaitingSpinner.showProgress()

            user.sendEmailVerification().addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.w(TAG, "email verification failed with error: ${task.exception?.message}")
                }

                WaitingSpinner.hideProgress()
                onCompletedCallback(task.isSuccessful)
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

    fun user(): FirebaseUserLocal {
        return currentUser
    }

    fun changeUserName(newUserName: String, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUserName)
            .setPhotoUri(currentUser.photoURL)
            .build()

        auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i(TAG, "User profile updated.")
                updateUserProfile(auth.currentUser)
                onCompletedCallback(true)
            } else {

                Log.w(TAG, "User profile update failed. Error: ${task.exception?.message}")
                onCompletedCallback(false)
            }
        }
    }

    fun reloadUserData(context: Context, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        auth.currentUser?.reload()?.addOnCompleteListener { task ->

            if (task.isSuccessful) {
                updateUserProfile(auth.currentUser)
            } else {
                Log.w(TAG, "reloading currentUser data failed with error: ${task.exception?.message}")
                Toast.makeText(context, context.getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_LONG).show()
            }

            onCompletedCallback(task.isSuccessful)
        }
    }

    fun usersAuthenticationStateText(context: Context): String {

        val isUserLoggedIn = currentUser.email != null && currentUser.id != null

        return if (isUserLoggedIn) {

            if (currentUser.isVerified) {
                context.getString(R.string.authentication_state_signed_in, currentUser.email)
            } else {
                context.getString(R.string.authentication_state_signed_in_but_missing_verification, currentUser.email)
            }

        } else {
            context.getString(R.string.authentication_state_signed_out)
        }
    }

    private fun updateUserProfile(user: FirebaseUser?) {

        if (user != null) {

            currentUser.name = user.displayName
            currentUser.email = user.email
            currentUser.isVerified = user.isEmailVerified
            currentUser.id = user.uid
            currentUser.photoURL = user.photoUrl

            userProfileObservers.forEach { it.value.get()?.userProfileChanged(currentUser) }

        } else {

            val emptyUserLocal = FirebaseUserLocal()
            currentUser = emptyUserLocal
            userProfileObservers.forEach { it.value.get()?.userProfileChanged(emptyUserLocal) }
        }
    }

    /**
     * data
     */

    private fun registerForNotificationToken(firebaseUser: FirebaseUser, onDataChanged: (data: String) -> Unit) {

        val databaseChildPath = FirebaseServer.DATABASE_USERS +"/"+firebaseUser.uid+"/"+ FirebaseServer.DATABASE_NOTIFICATION_TOKEN
        registerForData(databaseChildPath, onDataChanged)
    }

    private fun sendUserData(data: Map<String, String>) {
        currentUser.id?.let {
            FirebaseServer.sendData(currentUser)
        } ?: kotlin.run { Log.w(TAG, "update user data failed for data: $data, currentUser: $currentUser") }
    }

    fun registerForData(databaseChildPath: String, onDataChanged: (data: String) -> Unit) {

        // just if firebaseUser is logged in
        currentUser.let {

            FirebaseServer.database.child(databaseChildPath).addValueEventListener(object : ValueEventListener {

                override fun onCancelled(p0: DatabaseError) {
                    Log.w(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for data: $databaseChildPath")
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.d(TAG, "onDataChange(data: ${p0.value}) for data: $databaseChildPath")
                    (p0.value as? String)?.let { onDataChanged(it) }
                }
            })
        }
    }

    fun sendData(firebaseData: FirebaseData, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        Log.v(TAG, "update data [databaseChildPath: ${firebaseData.databasePath()}, data: ${firebaseData.data()}]")
        database.child(firebaseData.databasePath()).updateChildren(firebaseData.data()).addOnCompleteListener { onCompletedCallback(it.isSuccessful) }
    }

    /**
     * database constants
     */

    private val TAG = this.javaClass.name

    const val DATABASE_USERS = "users"
    const val DATABASE_ARENAS = "arenas"
    const val DATABASE_POKESTOPS = "pokestops"
    const val DATABASE_REG_USER = "registered_user"
    const val DATABASE_NOTIFICATION_TOKEN = "notificationToken"

    const val NOTIFICATION_DATA_LATITUDE = "latitude"
    const val NOTIFICATION_DATA_LONGITUDE = "longitude"

}