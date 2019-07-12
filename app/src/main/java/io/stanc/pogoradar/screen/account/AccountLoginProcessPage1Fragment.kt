package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentAccountLogin1Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.ViewModelFactory

class AccountLoginProcessPage1Fragment: Fragment() {
    private val TAG = javaClass.name

    private var viewModel = ViewModelFactory.getViewModel(LoginViewModel::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "Debug:: onCreateView(AccountLoginProcessPage1Fragment)")
        val binding = FragmentAccountLogin1Binding.inflate(inflater, container, false)
        Log.d(TAG, "Debug:: onCreateView(AccountLoginProcessPage1Fragment) viewModel: $viewModel, signType: ${viewModel?.signType?.get()?.name}")

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onDestroyView() {
        Log.d(TAG, "Debug:: onDestroyView(AccountLoginProcessPage1Fragment)")
        super.onDestroyView()
    }
}