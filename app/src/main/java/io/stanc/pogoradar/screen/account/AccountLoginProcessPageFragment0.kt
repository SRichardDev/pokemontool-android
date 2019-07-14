package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentAccountLogin0Binding
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.addOnPropertyChanged
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewpager.ViewPagerViewModel

class AccountLoginProcessPageFragment0: Fragment() {
    private val TAG = javaClass.name

    private var viewModel: LoginViewModel? = null
    private var viewPagerViewModel: ViewPagerViewModel? = null

    private var onEmailChangeCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin0Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
            viewPagerViewModel = ViewModelProviders.of(it).get(ViewPagerViewModel::class.java)

            onEmailChangeCallback = viewModel?.email?.addOnPropertyChanged {
                validateInput(it.get())
            }
        }

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        validateInput(viewModel?.email?.get())
    }

    override fun onDestroyView() {
        onEmailChangeCallback?.let { viewModel?.email?.removeOnPropertyChangedCallback(it) }
        super.onDestroyView()
    }

    private fun validateInput(email: String?) {

        if (FirebaseUser.isEmailValid(email)) {
            viewPagerViewModel?.onPageValidationChanged(page = 0, isValid = true)
        } else {
            viewPagerViewModel?.onPageValidationChanged(page = 0, isValid = false)
        }
    }
}