package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.firebase.node.FirebaseUserNode

class AccountViewModel: ViewModel() {
    private val TAG = javaClass.name

    enum class Team {
        MYSTIC,
        VALOR,
        INSTINCT
    }

    val name = ObservableField<String>()
    val email = ObservableField<String>()
    val level = ObservableField<String>("0")
    val team = ObservableField<Team>()

    val numberPokestops = ObservableField<Int>(0)
    val numberArenas = ObservableField<Int>(0)
    val numberRaids = ObservableField<Int>(0)
    val numberQuests = ObservableField<Int>(0)

    fun update(user: FirebaseUserNode) {

        name.set(user.trainerName)
        email.set(user.email)
        // TODO: refactore user data structure (firebase)
//        level.set(user.trainerLevel)
        teamOf(user.team)?.let { team.set(it) }
        numberPokestops.set(user.submittedPokestops.toInt())
        numberArenas.set(user.submittedArenas.toInt())
        numberRaids.set(user.submittedRaids.toInt())
        numberQuests.set(user.submittedQuests.toInt())

        Log.i(TAG, "Debug:: update($user), team: ${team.get()?.name}")
    }

    private fun teamOf(value: Number): Team? = Team.values().find { it.ordinal == value.toInt() }
}