package io.stanc.pogoradar.subscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.databinding.FragmentAccountLogin2Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountLoginFragment2: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin2Binding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: LoginViewModel): AccountLoginFragment2 {
            val fragment = AccountLoginFragment2()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}