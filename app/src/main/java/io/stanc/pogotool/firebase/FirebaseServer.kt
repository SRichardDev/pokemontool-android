package io.stanc.pogotool.firebase

import android.util.Log
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class FirebaseServer {

    private val firebase = FirebaseFirestore.getInstance()
    private var auth: FirebaseAuth? = null

    fun start() {
//        val firebase =
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