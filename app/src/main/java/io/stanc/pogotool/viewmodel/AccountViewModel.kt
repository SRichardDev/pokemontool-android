package io.stanc.pogotool.viewmodel

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.firebase.node.Team

class AccountViewModel: ViewModel() {
    private val TAG = javaClass.name

    val name = ObservableField<String>()
    val email = ObservableField<String>()
    val level = ObservableField<String>("0")
    val team = ObservableField<Team>()

    val numberPokestops = ObservableField<Int>(0)
    val numberArenas = ObservableField<Int>(0)
    val numberRaids = ObservableField<Int>(0)
    val numberQuests = ObservableField<Int>(0)

    fun update(user: FirebaseUserNode) {

        name.set(user.name)
        email.set(user.email)
        // TODO: refactore user data structure (firebase)
//        level.set(user.trainerLevel)
        team.set(user.team)
        numberPokestops.set(user.submittedPokestops.toInt())
        numberArenas.set(user.submittedArenas.toInt())
        numberRaids.set(user.submittedRaids.toInt())
        numberQuests.set(user.submittedQuests.toInt())

        Log.i(TAG, "Debug:: update($user), team: ${team.get()?.name}")
    }
}