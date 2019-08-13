package io.stanc.pogoradar.viewmodel

import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.App
import io.stanc.pogoradar.FirebaseImageMapper.TEAM_COLOR
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.firebase.node.Team
import io.stanc.pogoradar.utils.SegmentedControlView
import io.stanc.pogoradar.utils.addOnPropertyChanged

class LoginViewModel: ViewModel() {
    private val TAG = javaClass.name

    enum class SignType {
        SIGN_IN,
        SIGN_UP,
        PASSWORD_RESET
    }

    val signType = ObservableField<SignType>()
    private var signTypeChangeCallback: Observable.OnPropertyChangedCallback? = null

    val name = ObservableField<String>()
    val email = ObservableField<String>()
    val password = ObservableField<String>()
    val code = ObservableField<String>("")
    val level = ObservableField<String>("40")
    val team = ObservableField<Team>()
    val teamOrder = ObservableField<List<Team>>()
    val teamColor = ObservableField<Int>()

    val numberPokestops = ObservableField<Int>(0)
    val numberArenas = ObservableField<Int>(0)
    val numberRaids = ObservableField<Int>(0)
    val numberQuests = ObservableField<Int>(0)

    val emailLoginPageDescription = ObservableField<String>()
    val passwordLoginPageDescription = ObservableField<String>()

    init {
        if (signTypeChangeCallback == null) {
            signTypeChangeCallback = signType.addOnPropertyChanged { signType ->
                updateLoginPageDescriptions(signType.get())
            }
        }
    }

    fun update(user: FirebaseUserNode?) {

        user?.let {

            shuffleTeam()

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



        } ?: run {
            reset()
        }
    }

    private fun reset() {

        shuffleTeam()
        signType.set(null)

        name.set(null)
        email.set(null)
        code.set("")
        level.set("0")
        numberPokestops.set(0)
        numberArenas.set(0)
        numberRaids.set(0)
        numberQuests.set(0)
        emailLoginPageDescription.set(null)
        passwordLoginPageDescription.set(null)
    }

    private fun shuffleTeam() {
        val shuffledTeamOrder = Team.values().toList().shuffled()

        teamOrder.set(shuffledTeamOrder)
        team.set(shuffledTeamOrder[0])
        teamColor.set(TEAM_COLOR[shuffledTeamOrder[0]])
    }

    private fun updateLoginPageDescriptions(signType: SignType?) {

        when(signType) {

            SignType.SIGN_UP -> {
                emailLoginPageDescription.set(App.geString(R.string.authentication_login_email_description_signup))
                passwordLoginPageDescription.set(App.geString(R.string.authentication_login_password_description_signup))
            }

            SignType.SIGN_IN -> {
                emailLoginPageDescription.set(App.geString(R.string.authentication_login_email_description_signin))
                passwordLoginPageDescription.set(App.geString(R.string.authentication_login_password_description_signin))
            }

            SignType.PASSWORD_RESET -> {
                emailLoginPageDescription.set(App.geString(R.string.authentication_login_email_description_password_reset))
                passwordLoginPageDescription.set(null)
            }

            else -> {
                emailLoginPageDescription.set(null)
                passwordLoginPageDescription.set(null)
            }
        }
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