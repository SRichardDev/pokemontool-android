package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.stanc.pogotool.R
import io.stanc.pogotool.WaitingSpinner


object FirebaseServer {

//    TODO: refactor: use FirebaseFirestore instaed of FirebaseAuth
//    private val firebase = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * authentication methods
     */

    fun signUp(email: String, password: String, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        if (email.isEmpty() || password.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {task ->

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

//    fun testStoreCollection() {
//        // Create a new user with a first and last name
//        val user = HashMap()
//        user.put("first", "Ada")
//        user.put("last", "Lovelace")
//        user.put("born", 1815)
//
//// Add a new document with a generated ID
//        firebase.collection("users")
//            .add(user)
//            .addOnSuccessListener(OnSuccessListener<Any> { documentReference ->
//                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId()
//                )
//            })
//            .addOnFailureListener(OnFailureListener { e -> Log.w(TAG, "Error adding document", e) })
//    }
//
//    fun testAddDocToUsersCollection() {
//        // Create a new user with a first, middle, and last name
//        val user = HashMap()
//        user.put("first", "Alan")
//        user.put("middle", "Mathison")
//        user.put("last", "Turing")
//        user.put("born", 1912)
//
//// Add a new document with a generated ID
//        firebase.collection("users")
//            .add(user)
//            .addOnSuccessListener(OnSuccessListener<Any> { documentReference ->
//                Log.d(
//                    TAG,
//                    "DocumentSnapshot added with ID: " + documentReference.getId()
//                )
//            })
//            .addOnFailureListener(OnFailureListener { e -> Log.w(TAG, "Error adding document", e) })
//    }
//
//    fun testReadData() {
//        firebase.collection("users")
//            .get()
//            .addOnCompleteListener(OnCompleteListener<Any> { task ->
//                if (task.isSuccessful) {
//                    for (document in task.result!!) {
//                        Log.d(TAG, document.getId() + " => " + document.getData())
//                    }
//                } else {
//                    Log.w(TAG, "Error getting documents.", task.exception)
//                }
//            })
//    }

}