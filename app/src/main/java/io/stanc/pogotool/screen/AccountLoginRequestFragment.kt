package io.stanc.pogotool.screen

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.FirebaseUser.AuthState
import io.stanc.pogotool.firebase.node.Team
import io.stanc.pogotool.utils.ShowFragmentManager
import io.stanc.pogotool.viewmodel.LoginViewModel
import io.stanc.pogotool.viewmodel.LoginViewModel.SignType

class AccountLoginRequestFragment: Fragment() {
    private val TAG = javaClass.name

    private val viewModel = LoginViewModel()
    private var signInButton: Button? = null
    private var signUpButton: Button? = null
    private var verificationButton: Button? = null
    private var signOutButton: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_account_login, container, false)

        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))

        setupTeamImages(rootLayout)

        signInButton = rootLayout.findViewById(R.id.account_button_signin)
        signInButton?.setOnClickListener {
            viewModel.signType.set(SignType.SIGN_IN)
            val fragment = AccountLoginFragment.newInstance(viewModel)
            ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.activity_content_layout)
            // TODO: if successful, after Button:Send/ close -> show AccountInfoFragment
        }

        signUpButton = rootLayout.findViewById(R.id.account_button_signup)
        signUpButton?.setOnClickListener {
            viewModel.signType.set(SignType.SIGN_UP)
            val fragment = AccountLoginFragment.newInstance(viewModel)
            ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.activity_content_layout)
            // TODO: if successful, after Button:Send/ close -> show AccountInfoFragment
        }

        verificationButton = rootLayout.findViewById(R.id.account_button_verification)
        verificationButton?.setOnClickListener {
            FirebaseUser.sendEmailVerification { taskSuccessful, exception ->
                if (taskSuccessful) {
                    Toast.makeText(context, App.geString(R.string.authentication_state_verification_successful, FirebaseUser.userData?.email), Toast.LENGTH_LONG).show()
                } else {
                    Log.e(TAG, "sending email verification failed. exception: $exception")
                    Toast.makeText(context, App.geString(R.string.authentication_state_verification_failed), Toast.LENGTH_LONG).show()
                }
            }
        }

        signOutButton = rootLayout.findViewById(R.id.account_button_signout)
        signOutButton?.setOnClickListener {
            FirebaseUser.signOut()
        }

        return rootLayout
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

        viewModel.teamOrder.get()?.let { teamOrder ->

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
            when(newAuthState) {
                AuthState.UserLoggedIn -> {
                    signInButton?.visibility = View.GONE
                    signUpButton?.visibility = View.GONE
                    signOutButton?.visibility = View.VISIBLE
                    verificationButton?.visibility = View.GONE
                }
                AuthState.UserLoggedInButUnverified -> {
                    signInButton?.visibility = View.GONE
                    signUpButton?.visibility = View.GONE
                    signOutButton?.visibility = View.VISIBLE
                    verificationButton?.visibility = View.VISIBLE
                }
                AuthState.UserLoggedOut -> {
                    signInButton?.visibility = View.VISIBLE
                    signUpButton?.visibility = View.VISIBLE
                    signOutButton?.visibility = View.GONE
                    verificationButton?.visibility = View.GONE
                }
            }
        }
    }

}