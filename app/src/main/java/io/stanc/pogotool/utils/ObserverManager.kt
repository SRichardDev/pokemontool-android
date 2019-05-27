package io.stanc.pogotool.utils

import java.lang.ref.WeakReference

class ObserverManager<Observer> {
    private val TAG = javaClass.name

    private val observerMap = HashMap<Pair<Int, Any?>, WeakReference<Observer>>()

    fun addObserver(observer: Observer, subId: Any? = null) {
        val weakObserver = WeakReference(observer)
        observerMap[Pair(observer.hashCode(), subId)] = weakObserver
    }

    fun removeObserver(observer: Observer, subId: Any? = null) {
        observerMap.remove(Pair(observer.hashCode(), subId))
    }

    fun observers(subId: Any? = null): List<Observer?> = observerMap.entries.filter { it.key.second == subId }.associate { it.key to it.value.get() }.values.toList()
}