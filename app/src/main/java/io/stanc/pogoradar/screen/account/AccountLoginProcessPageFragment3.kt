package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentAccountLogin3Binding
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountLoginProcessPageFragment3: Fragment() {

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountLogin3Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
        }

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
}