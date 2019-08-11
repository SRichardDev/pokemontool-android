package io.stanc.pogoradar.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Async {

    interface Callback<T> {
        fun onCompletion(result: T)
        fun onException(e: Exception?)
    }

    suspend fun <T> awaitCallback(block: (Callback<T>) -> Unit) : T =
        suspendCancellableCoroutine { cont ->
            block(object : Callback<T> {
                override fun onCompletion(result: T) = cont.resume(result)
                override fun onException(e: Exception?) {
                    e?.let { cont.resumeWithException(it) }
                }
            })
        }

    suspend fun waitForCompletion(block: ((Boolean) -> Unit) -> Unit) : Boolean =
        suspendCancellableCoroutine { cont ->
            block(object: (Boolean) -> Unit {
                override fun invoke(isSuccessful: Boolean) {
                    cont.resume(isSuccessful)
                }
            })
        }
}