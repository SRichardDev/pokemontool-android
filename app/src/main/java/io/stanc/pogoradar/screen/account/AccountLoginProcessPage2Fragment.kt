package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentAccountLogin2Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.ViewModelFactory

class AccountLoginProcessPage2Fragment: Fragment() {
    private val TAG = javaClass.name

    private var viewModel = ViewModelFactory.getViewModel(LoginViewModel::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "Debug:: onCreateView(AccountLoginProcessPage2Fragment)")
        val binding = FragmentAccountLogin2Binding.inflate(inflater, container, false)
        Log.d(TAG, "Debug:: onCreateView(AccountLoginProcessPage2Fragment) viewModel: $viewModel, signType: ${viewModel?.signType?.get()?.name}")
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onDestroyView() {
        Log.d(TAG, "Debug:: onDestroyView(AccountLoginProcessPage2Fragment)")
        super.onDestroyView()
    }
}