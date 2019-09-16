package io.stanc.pogoradar.firebase

interface FirebaseNodeObserver<FirebaseNodeType> {
    fun onItemChanged(item: FirebaseNodeType)
    fun onItemRemoved(itemId: String)
}