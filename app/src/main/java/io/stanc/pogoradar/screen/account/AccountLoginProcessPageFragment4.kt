package io.stanc.pogoradar.screen.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentAccountLogin4Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountLoginProcessPageFragment4: Fragment() {
    private val TAG = javaClass.name

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin4Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
        }

        binding.viewModel = viewModel

        return binding.root
    }
}