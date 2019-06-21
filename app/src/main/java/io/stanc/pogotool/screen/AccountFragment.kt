package io.stanc.pogotool.screen

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogotool.AppSettings
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentAuthenticationBinding
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.viewmodel.AccountViewModel

class AccountFragment: Fragment() {

    private val viewModel = AccountViewModel()

    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAuthenticationBinding>(inflater, R.layout.fragment_authentication, container, false)
        binding.viewModel = viewModel
        binding.settings = AppSettings

//        rootView?.setOnClickListener { activity?.let { SystemUtils.hideKeyboard(it) } }

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
            Log.i(TAG, "Debug:: authStateChanged(${newAuthState.name})")
        }
    }

    private val userDataObserver = object: FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            Log.i(TAG, "Debug:: userDataChanged($user)")
            user?.let { viewModel.update(it) }
        }
    }

    companion object {

        private val TAG = this::class.java.name
    }
}