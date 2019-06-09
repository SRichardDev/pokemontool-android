package io.stanc.pogotool.utils

import android.os.CountDownTimer
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

object RefreshTimer {
    private val TAG = javaClass.name

    private val countDownTimerList = HashMap<Any, WeakReference<CountDownTimer>>()

    fun run(minutes: Long, id: Any, onFinished: () -> Unit) {

        removeFromList(id)

        val countDownTimer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(minutes), TimeUnit.MINUTES.toMillis(1)) {

            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                onFinished()
            }
        }

        addToList(id, countDownTimer)

        countDownTimer.start()
    }

    fun stop(id: Any) {
        removeFromList(id)
    }

    private fun removeFromList(id: Any) {
        countDownTimerList.remove(id)?.let {
            it.get()?.cancel()
        }
    }

    private fun addToList(id: Any, countDownTimer: CountDownTimer) {
        countDownTimerList[id] = WeakReference(countDownTimer)
    }
}