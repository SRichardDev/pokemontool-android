package io.stanc.pogotool.subscreen

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogotool.R
import io.stanc.pogotool.databinding.FragmentAccountLogin5Binding
import io.stanc.pogotool.viewmodel.LoginViewModel

class AccountLoginFragment5: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAccountLogin5Binding>(inflater, R.layout.fragment_account_login_5, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: LoginViewModel): AccountLoginFragment5 {
            val fragment = AccountLoginFragment5()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}