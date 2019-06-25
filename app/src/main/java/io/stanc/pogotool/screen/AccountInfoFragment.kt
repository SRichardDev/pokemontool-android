package io.stanc.pogotool.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.stanc.pogotool.AppSettings
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentAccountInfoBinding
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.viewmodel.LoginViewModel

class AccountInfoFragment: Fragment() {

    private val viewModel = LoginViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAccountInfoBinding>(inflater, R.layout.fragment_account_info, container, false)
        binding.viewModel = viewModel
        binding.settings = AppSettings

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(getString(R.string.authentication_app_title))
        FirebaseUser.addAuthStateObserver(authStateObserver)
        FirebaseUser.addUserDataObserver(userDataObserver)
    }

    override fun onPause() {
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        FirebaseUser.removeUserDataObserver(userDataObserver)
        super.onPause()
    }

    /**
     * firebase observer
     */

    private val authStateObserver = object: FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseUser.AuthState) {
            // TODO: on authStateChanged?
            Log.i(TAG, "Debug:: authStateChanged(${newAuthState.name})")
        }
    }

    private val userDataObserver = object: FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            user?.let { viewModel.update(it) }
        }
    }

    companion object {

        private val TAG = this::class.java.name
    }
}