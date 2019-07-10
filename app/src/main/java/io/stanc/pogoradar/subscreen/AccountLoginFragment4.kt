package io.stanc.pogoradar.subscreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentAccountLogin4Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountLoginFragment4: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin4Binding.inflate(inflater, container, false)
        binding.viewModel = viewModel

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