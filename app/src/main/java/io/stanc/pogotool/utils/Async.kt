package io.stanc.pogotool.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Async {

    interface Response<T> {
        fun onCompletion(result: T)
        fun onException(e: Exception?)
    }

    suspend fun <T> awaitResponse(block: (Response<T>) -> Unit) : T =
        suspendCancellableCoroutine { cont ->
            block(object : Response<T> {
                override fun onCompletion(result: T) = cont.resume(result)
                override fun onException(e: Exception?) {
                    e?.let { cont.resumeWithException(it) }
                }
            })
        }
}