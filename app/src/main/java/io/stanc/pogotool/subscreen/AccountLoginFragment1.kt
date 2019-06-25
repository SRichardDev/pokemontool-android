package io.stanc.pogotool.subscreen

import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogotool.R
import io.stanc.pogotool.databinding.FragmentAccountLogin1Binding
import io.stanc.pogotool.viewmodel.LoginViewModel

class AccountLoginFragment1: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAccountLogin1Binding>(inflater, R.layout.fragment_account_login_1, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: LoginViewModel): AccountLoginFragment1 {
            val fragment = AccountLoginFragment1()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}