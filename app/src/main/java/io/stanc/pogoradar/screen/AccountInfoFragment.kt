package io.stanc.pogoradar.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.AppSettings
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.databinding.FragmentAccountInfoBinding
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.LoginViewModel


class AccountInfoFragment: Fragment() {

    private val viewModel = LoginViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAccountInfoBinding>(inflater, R.layout.fragment_account_info, container, false)
        binding.viewModel = viewModel
        binding.settings = AppSettings

        binding.root.findViewById<Button>(R.id.account_info_button)?.setOnClickListener {
            ShowFragmentManager.showFragment(AccountInfoEditFragment.newInstance(viewModel), fragmentManager, R.id.account_layout)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(getString(R.string.authentication_app_title))
        AppbarManager.setMenuButton(R.string.authentication_button_sign_out, onMenuIconClicked = {
            FirebaseUser.signOut()
        })
        FirebaseUser.addUserDataObserver(userDataObserver)
    }

    override fun onPause() {
        FirebaseUser.removeUserDataObserver(userDataObserver)
        AppbarManager.setTitle(getString(R.string.default_app_title))
        AppbarManager.resetMenu()
        super.onPause()
    }

    /**
     * firebase observer
     */

    private val userDataObserver = object: FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            user?.let { viewModel.update(it) }
        }
    }

    companion object {

        private val TAG = this::class.java.name
    }
}