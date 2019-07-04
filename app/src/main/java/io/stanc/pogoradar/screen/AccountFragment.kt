package io.stanc.pogoradar.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.ShowFragmentManager

class AccountFragment: Fragment() {
    private val TAG = javaClass.name

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onResume() {
        super.onResume()
        FirebaseUser.addAuthStateObserver(authStateObserver)
    }

    override fun onPause() {
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        super.onPause()
    }

    /**
     * firebase observer
     */

    private val authStateObserver = object: FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseUser.AuthState) {
            when(newAuthState) {
                FirebaseUser.AuthState.UserLoggedIn -> showAccountInfoFragment()
                FirebaseUser.AuthState.UserLoggedInButUnverified -> showAccountLoginFragment()
                FirebaseUser.AuthState.UserLoggedOut -> showAccountLoginFragment()
            }
        }
    }

    private fun showAccountInfoFragment() {
        ShowFragmentManager.replaceFragment(AccountInfoFragment(), childFragmentManager, R.id.account_layout)
    }

    private fun showAccountLoginFragment() {
        ShowFragmentManager.replaceFragment(AccountLoginFragment(), childFragmentManager, R.id.account_layout)
    }
}