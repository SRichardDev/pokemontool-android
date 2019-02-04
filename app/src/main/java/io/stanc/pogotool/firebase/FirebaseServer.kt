package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import io.stanc.pogotool.R
import io.stanc.pogotool.WaitingSpinner
import io.stanc.pogotool.geohash.GeoHash
import com.google.android.gms.tasks.OnSuccessListener


object FirebaseServer {

//    TODO: refactor: use FirebaseFirestore instaed of FirebaseAuth
//    private val firebase = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

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
                Log.w(this.javaClass.name, "signing up failed with error: ${task.exception?.message}")
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
                Log.w(this.javaClass.name, "signing in failed with error: ${task.exception?.message}")
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
                    Log.w(this.javaClass.name, "email verification failed with error: ${task.exception?.message}")
                }

                WaitingSpinner.hideProgress()
                onCompletedCallback(task.isSuccessful)
            }
        } ?: kotlin.run { Log.w(this.javaClass.name, "can not send email verification because current user is null!") }
    }


    /**
     * user methods
     */

    fun updateUserData(context: Context, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        auth.currentUser?.reload()?.addOnCompleteListener { task ->

            if (!task.isSuccessful) {
                Log.w(this.javaClass.name, "reloading user data failed with error: ${task.exception?.message}")
            }

            onCompletedCallback(task.isSuccessful)

            if (!task.isSuccessful) {
                Toast.makeText(context, context.getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Get user info if logged in, null otherwise
     */
    fun user(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * data registration
     */

    fun registerForUserName(onUserNameChanged: (userName: String) -> Unit) {
        registerForUserData(DATABASE_USER_TRAINER_NAME, onUserNameChanged)
    }

    private fun registerForUserData(dataKey: String, onDataChanged: (data: String) -> Unit) {

        auth.currentUser?.let { user ->
            database.child(DATABASE_USERS).child(user.uid).child(dataKey).addValueEventListener(object : ValueEventListener {

                override fun onCancelled(p0: DatabaseError) {
                    Log.w(this.javaClass.name, "onCancelled(error: ${p0.code}, message: ${p0.message})")
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.d(this.javaClass.name, "onDataChange(data: ${p0.value})")
                    (p0.value as? String)?.let { onDataChanged(it) }
                }
            })
        }
    }

    private fun registerForArea(geoHash: GeoHash, onDataChanged: (data: String) -> Unit) {

        auth.currentUser?.let { user ->
            database.child(DATABASE_ARENAS).child(geoHash.hashCode().toString()).addValueEventListener(object : ValueEventListener {

                override fun onCancelled(p0: DatabaseError) {
                    Log.w(this.javaClass.name, "onCancelled(error: ${p0.code}, message: ${p0.message})")
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.d(this.javaClass.name, "onDataChange(data: ${p0.value})")
                    (p0.value as? String)?.let { onDataChanged(it) }
                }
            })
        }
    }

    fun subscribeForPush(geohash: GeoHash) {

        auth.currentUser?.let { user ->

            user.getIdToken(false).addOnSuccessListener { taskSnapshot ->
                Log.d(this.javaClass.name, "getIdToken.addOnSuccessListener -> ${taskSnapshot.token}")
                taskSnapshot.token?.let { token ->

                    val data = HashMap<String, String>()
                    data[token] = user.uid

                    updateData(DATABASE_ARENAS, data, geohash)
                    updateData(DATABASE_POKESTOPS, data, geohash)
                    updateData(DATABASE_RAID_BOSSES, data, geohash)
                }

            }
        }

    }

    fun updateData(type: String, data: Map<String, String>, geoHash: GeoHash) {
        database.child(type).child(geoHash.hashCode().toString()).child(DATABASE_REG_USER).updateChildren(data)
    }

    fun usersAuthenticationStateText(context: Context): String {

        return FirebaseServer.user()?.let { user ->

            if (user.isEmailVerified) {
                context.getString(R.string.authentication_state_signed_in, user.email)
            } else {
                context.getString(R.string.authentication_state_signed_in_but_missing_verification, user.email)
            }

        } ?: kotlin.run {
            context.getString(R.string.authentication_state_signed_out)
        }
    }

    fun updateUserName(name: String, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        auth.currentUser?.let { user ->

            val data = HashMap<String, Any>()
            data[DATABASE_USER_TRAINER_NAME] = name

            database.database.getReference(DATABASE_USERS).child(user.uid).updateChildren(data).addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.w(this.javaClass.name, "update user name failed with error: ${task.exception?.message}")
                }

                onCompletedCallback(task.isSuccessful)
            }
        }
    }

    /**
     * database
     */

    private const val DATABASE_USERS = "users"
    private const val DATABASE_USER_TRAINER_NAME = "trainerName"
    private const val DATABASE_ARENAS = "arenas"
    private const val DATABASE_POKESTOPS = "pokestops"
    private const val DATABASE_RAID_BOSSES = "raidBosses"
    private const val DATABASE_REG_USER = "registered_user"

}