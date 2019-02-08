package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.*
import io.stanc.pogotool.NavDrawerActivity
import io.stanc.pogotool.WaitingSpinner
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.R
import java.lang.ref.WeakReference


object FirebaseServer {

//    TODO: refactor: use FirebaseFirestore instaed of FirebaseAuth
//    private val firebase = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private var currentUserLocal: io.stanc.pogotool.firebase.FirebaseUserLocal? = null

    fun start() {
        auth.addAuthStateListener { onAuthStateChanged(it) }
    }

    fun stop() {
        auth.removeAuthStateListener { onAuthStateChanged }
    }


    /**
     * authentication listener
     */

    private val authStateObservers = mutableListOf<WeakReference<AuthStateObserver>>()

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
        authStateObservers.add(weakObserver)
    }

    fun removeAuthStateObserver(observer: AuthStateObserver) {
        val weakObserver = WeakReference(observer)

        val checkCount = authStateObservers.count()
        authStateObservers.remove(weakObserver)
        val newCheckCount = authStateObservers.count()
        if (newCheckCount >= checkCount) {
            Log.e(TAG, "removing observer failed: checkCount: $checkCount, newCheckCount: $newCheckCount")
        }
    }

    private val onAuthStateChanged: (firebaseAuth: FirebaseAuth) -> Unit = { firebaseAuth ->

        updateUserProfile(firebaseAuth.currentUser)

        firebaseAuth.currentUser?.let { firebaseUser ->

            Log.i(TAG, "onAuthStateChanged: $currentUserLocal")
            if (firebaseUser.isEmailVerified) {
                authStateObservers.forEach { it.get()?.authStateChanged(AuthState.UserLoggedIn) }
            } else {
                authStateObservers.forEach { it.get()?.authStateChanged(AuthState.UserLoggedInButUnverified) }
            }

        } ?: kotlin.run {
            Log.i(TAG, "onAuthStateChanged: $currentUserLocal")
            authStateObservers.forEach { it.get()?.authStateChanged(AuthState.UserLoggedOut) }
        }
    }

    /**
     * user profile listener
     */

    private val userProfileObservers = mutableListOf<WeakReference<UserProfileObserver>>()

    interface UserProfileObserver {
        fun userProfileChanged(user: FirebaseUserLocal)
    }

    //  TODO: see https://firebase.google.com/docs/auth/web/manage-users
    fun addUserProfileObserver(observer: UserProfileObserver) {
        val weakObserver = WeakReference(observer)
        userProfileObservers.add(weakObserver)
    }

    fun removeUserProfileObserver(observer: UserProfileObserver) {
        val weakObserver = WeakReference(observer)

        val checkCount = userProfileObservers.count()
        userProfileObservers.remove(weakObserver)
        val newCheckCount = userProfileObservers.count()
        if (newCheckCount >= checkCount) {
            Log.e(TAG, "removing observer failed: checkCount: $checkCount, newCheckCount: $newCheckCount")
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
        } ?: kotlin.run { Log.w(TAG, "can not send email verification because current currentUserLocal is null!") }
    }

    fun deleteUser() {
        // TODO...
//        ...
//        updateUserProfile()
    }

    /**
     * currentUserLocal methods
     */

    fun updateUserData(context: Context, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        Log.d(TAG, "Debug:: updateUserData()")

        auth.currentUser?.reload()?.addOnCompleteListener { task ->

            if (task.isSuccessful) {
                updateUserProfile(auth.currentUser)
            } else {
                Log.w(TAG, "reloading currentUserLocal data failed with error: ${task.exception?.message}")
                Toast.makeText(context, context.getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_SHORT).show()
            }

            onCompletedCallback(task.isSuccessful)
        }
    }

    private fun updateUserProfile(user: FirebaseUser?) {

        Log.d(TAG, "Debug:: updateUserProfile(${user?.displayName})")
        if (user != null) {

            FirebaseUserLocal(
                user.displayName,
                user.email,
                user.photoUrl,
                user.isEmailVerified
            ).let { userLocal ->

                currentUserLocal = userLocal
                userProfileObservers.forEach { it.get()?.userProfileChanged(userLocal) }
            }

        } else {
            currentUserLocal = null
            userProfileObservers.forEach { it.get()?.userProfileChanged(FirebaseUserLocal(null, null, null, false)) }
        }
    }

    /**
     * Get currentUserLocal info if logged in, null otherwise
     */
    fun user(): FirebaseUserLocal? {
        return currentUserLocal
    }

    /**
     * data registration
     */

    fun changeUserName(newUserName: String, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUserName)
            .setPhotoUri(currentUserLocal?.photoURL)
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

    private fun registerForArea(geoHash: GeoHash, onDataChanged: (data: String) -> Unit) {

        auth.currentUser?.let { user ->
            database.child(DATABASE_ARENAS).child(geoHash.hashCode().toString()).addValueEventListener(object : ValueEventListener {

                override fun onCancelled(p0: DatabaseError) {
                    Log.w(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message})")
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.d(TAG, "onDataChange(data: ${p0.value})")
                    (p0.value as? String)?.let { onDataChanged(it) }
                }
            })
        }
    }

    fun subscribeForPush(geohash: GeoHash) {

        auth.currentUser?.let { user ->

            user.getIdToken(false).addOnSuccessListener { taskSnapshot ->
                Log.d(TAG, "getIdToken.addOnSuccessListener -> ${taskSnapshot.token}")
//                taskSnapshot.token?.let { token ->
//
//                    val data = HashMap<String, String>()
//                    data[token] = user.uid
//
//                    updateData(DATABASE_ARENAS, data, geohash)
//                    updateData(DATABASE_POKESTOPS, data, geohash)
//                    updateData(DATABASE_RAID_BOSSES, data, geohash)
//                }
            }
        }
    }

    fun updateData(type: String, data: Map<String, String>, geoHash: GeoHash) {
        database.child(type).child(geoHash.hashCode().toString()).child(DATABASE_REG_USER).updateChildren(data)
    }

    fun usersAuthenticationStateText(context: Context): String {

        return FirebaseServer.user()?.let { user ->

            if (user.isVerified) {
                context.getString(R.string.authentication_state_signed_in, user.email)
            } else {
                context.getString(R.string.authentication_state_signed_in_but_missing_verification, user.email)
            }

        } ?: kotlin.run {
            context.getString(R.string.authentication_state_signed_out)
        }
    }

    /**
     * database
     */

    private val TAG = this.javaClass.name

    private const val DATABASE_USERS = "users"
    private const val DATABASE_USER_TRAINER_NAME = "trainerName"
    private const val DATABASE_ARENAS = "arenas"
    private const val DATABASE_POKESTOPS = "pokestops"
    private const val DATABASE_RAID_BOSSES = "raidBosses"
    private const val DATABASE_REG_USER = "registered_user"

}