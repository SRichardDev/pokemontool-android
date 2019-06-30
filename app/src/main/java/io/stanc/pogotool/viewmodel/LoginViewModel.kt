package io.stanc.pogotool.viewmodel

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import android.util.Log
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.firebase.node.Team
import io.stanc.pogotool.utils.SegmentedControlView

class LoginViewModel: ViewModel() {
    private val TAG = javaClass.name

    enum class SignType {
        SIGN_IN,
        SIGN_UP
    }

    val signType = ObservableField<SignType>()

    val name = ObservableField<String>()
    val email = ObservableField<String>()
    val password = ObservableField<String>()
    val code = ObservableField<String>("0000 0000 0000")
    val level = ObservableField<String>("0")
    val team = ObservableField<Team>()
    val teamOrder = ObservableField<List<Team>>()
    val teamColor = ObservableField<Int>()

    val numberPokestops = ObservableField<Int>(0)
    val numberArenas = ObservableField<Int>(0)
    val numberRaids = ObservableField<Int>(0)
    val numberQuests = ObservableField<Int>(0)

    private val teamColorMap = mapOf(Team.MYSTIC to R.color.teamMystic, Team.VALOR to R.color.teamValor, Team.INSTINCT to R.color.teamInstinct)

    init {

        val shuffledTeamOrder = Team.values().toList().shuffled()

        teamOrder.set(shuffledTeamOrder)

        team.set(shuffledTeamOrder[0])
        teamColor.set(teamColorMap[shuffledTeamOrder[0]])
    }

    fun update(user: FirebaseUserNode) {

        name.set(user.name)
        email.set(user.email)
        user.code?.let { code.set(it) }
        level.set(user.level.toString())
        team.set(user.team)
        teamColor.set(teamColorMap[user.team])
        numberPokestops.set(user.submittedPokestops.toInt())
        numberArenas.set(user.submittedArenas.toInt())
        numberRaids.set(user.submittedRaids.toInt())
        numberQuests.set(user.submittedQuests.toInt())

    }

    fun onSelectedStateDidChange(selection: SegmentedControlView.Selection) {

        when(selection) {
            SegmentedControlView.Selection.LEFT -> {
                teamOrder.get()?.get(0)?.let {
                    team.set(it)
                    teamColor.set(teamColorMap[it])
                }
            }
            SegmentedControlView.Selection.MIDDLE -> {
                teamOrder.get()?.get(1)?.let {
                    team.set(it)
                    teamColor.set(teamColorMap[it])
                }
            }
            SegmentedControlView.Selection.RIGHT -> {
                teamOrder.get()?.get(2)?.let {
                    team.set(it)
                    teamColor.set(teamColorMap[it])
                }
            }
        }
    }
}