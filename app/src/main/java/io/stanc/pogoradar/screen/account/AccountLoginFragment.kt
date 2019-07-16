package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.FirebaseUser.AuthState
import io.stanc.pogoradar.firebase.node.Team
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.LoginViewModel.SignType

class AccountLoginFragment: Fragment() {
    private val TAG = javaClass.name

    private var viewModel: LoginViewModel? = null

    private var subTextView: TextView? = null

    private var signInButton: Button? = null
    private var signUpButton: Button? = null
    private var passwordResetButton: Button? = null
    private var signOutButton: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_account_login, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
        }

        setupTeamImages(rootLayout)

        subTextView = rootLayout.findViewById(R.id.account_sub_text)

        signInButton = rootLayout.findViewById(R.id.account_button_signin)
        signInButton?.setOnClickListener {
            viewModel?.signType?.set(SignType.SIGN_IN)
            ShowFragmentManager.showFragment(AccountLoginProcessFragment(), fragmentManager, R.id.account_layout)
        }

        signUpButton = rootLayout.findViewById(R.id.account_button_signup)
        signUpButton?.setOnClickListener {
            viewModel?.signType?.set(SignType.SIGN_UP)
            ShowFragmentManager.showFragment(AccountLoginProcessFragment(), fragmentManager, R.id.account_layout)
        }

        passwordResetButton = rootLayout.findViewById(R.id.account_button_reset_password)
        passwordResetButton?.setOnClickListener {
            viewModel?.signType?.set(SignType.PASSWORD_RESET)
            ShowFragmentManager.showFragment(AccountLoginProcessFragment(), fragmentManager, R.id.account_layout)
        }

        signOutButton = rootLayout.findViewById(R.id.account_button_signout)
        signOutButton?.setOnClickListener {
            FirebaseUser.signOut()
        }

        return rootLayout
    }

    override fun onStart() {
        super.onStart()
        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))
    }

    override fun onResume() {
        super.onResume()
        FirebaseUser.addAuthStateObserver(authStateObserver)
    }

    override fun onPause() {
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        super.onPause()
    }

    private fun setupTeamImages(rootLayout: View) {

        val teamImageMap = mapOf(Team.INSTINCT to R.drawable.icon_instinct_512dp,
                                                Team.MYSTIC to R.drawable.mystic,
                                                Team.VALOR to R.drawable.icon_valor_512dp)

        viewModel?.teamOrder?.get()?.let { teamOrder ->

            if (teamOrder.size != 3) {
                Log.e(TAG, "viewModel.teamOrder contains not 3 entries! teamOrder: $teamOrder")
                return
            }

            teamImageMap[teamOrder[0]]?.let { rootLayout.findViewById<ImageView>(R.id.account_imageView_0).setImageResource(it) }
            teamImageMap[teamOrder[1]]?.let { rootLayout.findViewById<ImageView>(R.id.account_imageView_1).setImageResource(it) }
            teamImageMap[teamOrder[2]]?.let { rootLayout.findViewById<ImageView>(R.id.account_imageView_2).setImageResource(it) }
        }
    }

    private val authStateObserver = object : FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: AuthState) {
            Log.i(TAG, "authStateChanged(${newAuthState.name})")
            when(newAuthState) {
                AuthState.UserLoggedIn -> {
                    signInButton?.visibility = View.GONE
                    signUpButton?.visibility = View.GONE
                    signOutButton?.visibility = View.VISIBLE
                    passwordResetButton?.visibility = View.GONE
                    subTextView?.visibility = View.GONE
                }
                AuthState.UserLoggedInButUnverified -> {
                    signInButton?.visibility = View.GONE
                    signUpButton?.visibility = View.GONE
                    signOutButton?.visibility = View.VISIBLE
                    passwordResetButton?.visibility = View.GONE
                    subTextView?.visibility = View.VISIBLE
                    subTextView?.text = App.geString(R.string.authentication_state_signed_in_but_missing_verification)
                }
                AuthState.UserLoggedOut -> {
                    signInButton?.visibility = View.VISIBLE
                    signUpButton?.visibility = View.VISIBLE
                    signOutButton?.visibility = View.GONE
                    passwordResetButton?.visibility = View.VISIBLE
                    subTextView?.visibility = View.VISIBLE
                    subTextView?.text = App.geString(R.string.authentication_state_signed_out)
                }
            }
        }
    }

}