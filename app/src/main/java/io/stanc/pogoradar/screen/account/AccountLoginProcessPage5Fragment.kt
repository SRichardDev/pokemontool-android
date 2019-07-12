package io.stanc.pogoradar.screen.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogoradar.databinding.FragmentAccountLogin5Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.ViewModelFactory

class AccountLoginProcessPage5Fragment: Fragment() {
    private val TAG = javaClass.name

    private var viewModel = ViewModelFactory.getViewModel(LoginViewModel::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin5Binding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        return binding.root
    }
}