package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentAccountLogin1Binding
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.addOnPropertyChanged
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewpager.ViewPagerViewModel

class AccountLoginProcessPageFragment1: Fragment() {
    private val TAG = javaClass.name

    private var viewModel: LoginViewModel? = null
    private var viewPagerViewModel: ViewPagerViewModel? = null

    private var onPasswordChangeCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin1Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
            viewPagerViewModel = ViewModelProviders.of(it).get(ViewPagerViewModel::class.java)

            onPasswordChangeCallback = viewModel?.password?.addOnPropertyChanged {
                validateInput(it.get())
            }
        }

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        validateInput(viewModel?.password?.get())
    }

    override fun onDestroyView() {
        onPasswordChangeCallback?.let { viewModel?.password?.removeOnPropertyChangedCallback(it) }
        super.onDestroyView()
    }

    private fun validateInput(password: String?) {

        if (FirebaseUser.isPasswordValid(password)) {
            viewPagerViewModel?.onPageValidationChanged(page = 1, isValid = true)
        } else {
            viewPagerViewModel?.onPageValidationChanged(page = 1, isValid = false)
        }
    }
}