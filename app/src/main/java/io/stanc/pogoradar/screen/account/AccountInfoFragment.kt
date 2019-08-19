package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.databinding.FragmentAccountInfoBinding
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.notification.NotificationSettings
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.LoginViewModel


class AccountInfoFragment: Fragment() {
    private val TAG = this::class.java.name

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountInfoBinding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
        }

        binding.viewModel = viewModel
        binding.settings = NotificationSettings

        binding.root.findViewById<Button>(R.id.account_info_button)?.setOnClickListener {
            ShowFragmentManager.showFragment(AccountInfoEditFragment(), fragmentManager, R.id.account_layout)
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setMenuButton(R.string.authentication_button_sign_out, onMenuIconClicked = {
            FirebaseUser.signOut()
        })
        FirebaseUser.addUserDataObserver(userDataObserver)
    }

    override fun onPause() {
        FirebaseUser.removeUserDataObserver(userDataObserver)
        AppbarManager.resetMenu()
        super.onPause()
    }

    /**
     * firebase observer
     */

    private val userDataObserver = object: FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            user?.let { viewModel?.update(it) }
        }
    }
}