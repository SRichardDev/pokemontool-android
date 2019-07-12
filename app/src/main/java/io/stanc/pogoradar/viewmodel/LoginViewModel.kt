package io.stanc.pogoradar.viewmodel

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.FirebaseImageMapper.TEAM_COLOR
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.firebase.node.Team
import io.stanc.pogoradar.utils.SegmentedControlView

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

    init {
        Log.i(TAG, "Debug:: LoginViewModel created !!!")
        val shuffledTeamOrder = Team.values().toList().shuffled()

        teamOrder.set(shuffledTeamOrder)

        team.set(shuffledTeamOrder[0])
        teamColor.set(TEAM_COLOR[shuffledTeamOrder[0]])
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "Debug:: LoginViewModel destroyed!")
    }

    fun update(user: FirebaseUserNode) {

        name.set(user.name)
        email.set(user.email)
        user.code?.let { code.set(it) }
        level.set(user.level.toString())
        team.set(user.team)
        teamColor.set(TEAM_COLOR[user.team])
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
                    teamColor.set(TEAM_COLOR[it])
                }
            }
            SegmentedControlView.Selection.MIDDLE -> {
                teamOrder.get()?.get(1)?.let {
                    team.set(it)
                    teamColor.set(TEAM_COLOR[it])
                }
            }
            SegmentedControlView.Selection.RIGHT -> {
                teamOrder.get()?.get(2)?.let {
                    team.set(it)
                    teamColor.set(TEAM_COLOR[it])
                }
            }
        }
    }
}