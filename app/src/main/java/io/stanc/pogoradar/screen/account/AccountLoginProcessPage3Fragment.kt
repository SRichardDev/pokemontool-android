package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.databinding.FragmentAccountLogin3Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.ViewModelFactory

class AccountLoginProcessPage3Fragment: Fragment() {

    private var viewModel = ViewModelFactory.getViewModel(LoginViewModel::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin3Binding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        return binding.root
    }
}