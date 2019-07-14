package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentAccountLogin2Binding
import io.stanc.pogoradar.utils.addOnPropertyChanged
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewpager.ViewPagerViewModel

class AccountLoginProcessPageFragment2: Fragment() {

    private var viewModel: LoginViewModel? = null
    private var viewPagerViewModel: ViewPagerViewModel? = null

    private var onNameChangeCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin2Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
            viewPagerViewModel = ViewModelProviders.of(it).get(ViewPagerViewModel::class.java)

            onNameChangeCallback = viewModel?.name?.addOnPropertyChanged {
                validateInput(it.get())
            }
        }

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        validateInput(viewModel?.name?.get())
    }

    override fun onDestroyView() {
        onNameChangeCallback?.let { viewModel?.name?.removeOnPropertyChangedCallback(it) }
        super.onDestroyView()
    }

    private fun validateInput(name: String?) {
        if (name?.isNotBlank() == true) {
            viewPagerViewModel?.onPageValidationChanged(page = 2, isValid = true)
        } else {
            viewPagerViewModel?.onPageValidationChanged(page = 2, isValid = false)
        }
    }
}