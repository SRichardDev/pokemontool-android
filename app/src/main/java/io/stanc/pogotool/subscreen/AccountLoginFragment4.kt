package io.stanc.pogotool.subscreen

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import io.stanc.pogotool.R
import io.stanc.pogotool.databinding.FragmentAccountLogin4Binding
import io.stanc.pogotool.viewmodel.LoginViewModel

class AccountLoginFragment4: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentAccountLogin4Binding>(inflater, R.layout.fragment_account_login_4, container, false)
        binding.viewModel = viewModel

        Log.i(TAG, "Debug:: onCreateView() viewModel.teamOrder: ${viewModel?.teamOrder}")

        binding.root.findViewById<NumberPicker>(R.id.account_numberpicker_level)?.let { numberPicker ->
            numberPicker.minValue = 1
            numberPicker.maxValue = 40
            viewModel?.level?.get()?.let { numberPicker.value = it.toInt() }
            numberPicker.setOnValueChangedListener { _, _, value ->
                viewModel?.level?.set(value.toString())
            }
        }

        return binding.root
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: LoginViewModel): AccountLoginFragment4 {
            val fragment = AccountLoginFragment4()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}