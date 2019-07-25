package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.App
import io.stanc.pogoradar.Popup
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.SystemUtils
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.LoginViewModel.SignType
import io.stanc.pogoradar.viewpager.ViewPagerFragment

class AccountLoginProcessFragment: ViewPagerFragment() {
    private val TAG = this::class.java.name

    private var viewModel: LoginViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.let {
            viewModel = ViewModelProviders.of(it).get(LoginViewModel::class.java)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override val viewPagerAdapter: FragmentPagerAdapter by lazy {
        AccountLoginProcessFragmentPagerAdapter(childFragmentManager, viewModel?.signType?.get()!!)
    }

    override fun navigationButtonClickedOnTheLastPage() {
        try {
            tryToSendLoginData()

        } catch (e: Exception) {
            // TODO: Popup implementation
            Popup.showToast(context, e.message)
        }
    }

    override fun onPageChanged(position: Int) {
        activity?.let { SystemUtils.hideKeyboard(it) }
    }

    private fun tryToSendLoginData() {
        try {
            viewModel?.let { viewModel ->

                when(viewModel.signType.get()) {

                    SignType.SIGN_IN -> {
                        FirebaseUser.signIn(viewModel.email.get()!!, viewModel.password.get()!!, signInCompletionCallback)
                    }

                    SignType.SIGN_UP -> {

                        val userConfig = FirebaseUser.UserLoginConfig(viewModel.email.get()!!,
                            viewModel.password.get()!!,
                            viewModel.name.get()!!,
                            viewModel.team.get()!!,
                            viewModel.level.get()!!.toInt())

                        FirebaseUser.signUp(userConfig, signUpCompletionCallback)
                    }

                    SignType.PASSWORD_RESET -> {
                        FirebaseUser.resetPassword(viewModel.email.get()!!, passwordResetCompletionCallback)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "could not sending login data! exception: ${e.message}")
            throw Exception(App.geString(R.string.exceptions_login_missing_data))
        }
    }

    private val signUpCompletionCallback = object: (Boolean, String?) -> Unit {
        override fun invoke(taskSuccessful: Boolean, exception: String?) {
            if (taskSuccessful) {
                close()
                Popup.showInfo(context, R.string.authentication_state_successful_signup, R.string.authentication_state_successful_signup_subtext)
            } else {
                val message = exception ?: App.geString(R.string.authentication_state_authentication_failed)
                Popup.showToast(context, message)
            }
        }
    }

    private val signInCompletionCallback = object: (Boolean, String?) -> Unit {
        override fun invoke(taskSuccessful: Boolean, exception: String?) {
            if (taskSuccessful) {
                close()
                Popup.showInfo(context, R.string.authentication_state_successful_signin)
            } else {
                val message = exception ?: App.geString(R.string.authentication_state_authentication_failed)
                Popup.showToast(context, message)
            }
        }
    }

    private val passwordResetCompletionCallback = object: (Boolean, String?) -> Unit {
        override fun invoke(taskSuccessful: Boolean, exception: String?) {
            if (taskSuccessful) {
                close()
                Popup.showInfo(context, R.string.authentication_state_successful_reset_password, R.string.authentication_state_successful_reset_password_subtext)
            } else {
                val message = exception ?: App.geString(R.string.authentication_state_password_reset_failed)
                Popup.showToast(context, message)
            }
        }
    }

    private fun close() {
        activity?.let { SystemUtils.hideKeyboard(it) }
        fragmentManager?.popBackStack()
    }
}