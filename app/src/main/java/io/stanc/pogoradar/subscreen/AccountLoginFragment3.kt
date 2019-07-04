package io.stanc.pogoradar.subscreen

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentAccountLogin3Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountLoginFragment3: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAccountLogin3Binding>(inflater, R.layout.fragment_account_login_3, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: LoginViewModel): AccountLoginFragment3 {
            val fragment = AccountLoginFragment3()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}