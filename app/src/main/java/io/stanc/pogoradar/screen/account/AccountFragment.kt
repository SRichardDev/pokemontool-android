package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountFragment: Fragment() {
    private val TAG = javaClass.name

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(R.layout.fragment_account, container, false)

        activity?.let {
            val viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
            viewModel.update(FirebaseUser.userData)
            Log.d(TAG, "Debug:: onCreateView(AccountFragment) viewModel: $viewModel, signType: ${viewModel.signType.get()?.name}")
        }

        return rootLayout
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Debug:: onResume(AccountFragment), childFragmentManager(${childFragmentManager.fragments.size}): ${childFragmentManager.fragments}, fragmentManager(${fragmentManager?.fragments?.size}): ${fragmentManager?.fragments})")
        FirebaseUser.addAuthStateObserver(authStateObserver)
    }

    override fun onPause() {
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "Debug:: onDestroyView(AccountFragment)")
        super.onDestroyView()
    }

    /**
     * firebase observer
     */

    private val authStateObserver = object: FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseUser.AuthState) {
            Log.i(TAG, "Debug:: authStateChanged(${newAuthState.name}, childFragmentManager(${childFragmentManager.fragments.size}): ${childFragmentManager.fragments}, fragmentManager(${fragmentManager?.fragments?.size}): ${fragmentManager?.fragments})")
            when(newAuthState) {
                FirebaseUser.AuthState.UserLoggedIn -> ShowFragmentManager.replaceFragment(AccountInfoFragment(), childFragmentManager, R.id.account_layout)
                FirebaseUser.AuthState.UserLoggedInButUnverified -> ShowFragmentManager.replaceFragment(AccountLoginFragment(), childFragmentManager, R.id.account_layout)
                FirebaseUser.AuthState.UserLoggedOut -> ShowFragmentManager.replaceFragment(AccountLoginFragment(), childFragmentManager, R.id.account_layout)
            }
        }
    }
}