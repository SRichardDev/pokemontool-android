package io.stanc.pogotool.utils

import kotlinx.coroutines.*

class DelayedTrigger(private val delayInMilliSec: Long,
                     private val action: () -> Unit) {

    private var timer: Job? = null

    fun trigger() {

        if (timer == null) {
            timer = GlobalScope.launch(Dispatchers.Default) {
                triggerSoon()
            }
        }

        timer?.let { job ->

            if (!job.isActive) {
                timer = GlobalScope.launch(Dispatchers.Default) {
                    triggerSoon()
                }
            }
        }
    }

    private suspend fun triggerSoon() {
        delay(delayInMilliSec)
        CoroutineScope(Dispatchers.Main).launch { action() }
    }
}