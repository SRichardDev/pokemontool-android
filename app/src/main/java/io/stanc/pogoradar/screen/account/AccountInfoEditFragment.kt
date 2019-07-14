package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentAccountInfoEditBinding
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.SystemUtils
import io.stanc.pogoradar.viewmodel.LoginViewModel

class AccountInfoEditFragment: Fragment() {
    private val TAG = javaClass.name

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAccountInfoEditBinding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
        }

        binding.viewModel = viewModel

        binding.root.findViewById<NumberPicker>(R.id.account_info_edit_numberpicker_level)?.let { numberPicker ->
            numberPicker.minValue = 1
            numberPicker.maxValue = 40
            viewModel?.level?.get()?.let { numberPicker.value = it.toInt() }
            numberPicker.setOnValueChangedListener { _, _, value ->
                viewModel?.level?.set(value.toString())
            }
        }

        binding.root.findViewById<Button>(R.id.account_info_edit_button)?.setOnClickListener {
            saveAndClose()
        }

        return binding.root
    }

    private fun updateUserData() {

        try {

            val userProfileConfig = FirebaseUser.UserProfileConfig(
                viewModel?.name?.get()!!,
                viewModel?.code?.get()!!,
                viewModel?.team?.get()?.toNumber()!!,
                viewModel?.level?.get()?.toInt()!!)

            FirebaseUser.updateUserProfile(userProfileConfig)

        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }
    }

    private fun saveAndClose() {
        updateUserData()
        activity?.let { SystemUtils.hideKeyboard(it) }
        fragmentManager?.popBackStack()
    }
}