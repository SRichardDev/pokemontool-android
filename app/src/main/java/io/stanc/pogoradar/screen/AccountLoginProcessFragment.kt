package io.stanc.pogoradar.screen

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.SystemUtils
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.LoginViewModel.SignType
import io.stanc.pogoradar.viewpager.ViewPagerFragment

class AccountLoginProcessFragment: ViewPagerFragment() {

    var viewModel: LoginViewModel? = null

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))
    }

    override fun onPause() {
        AppbarManager.setTitle(getString(R.string.default_app_title))
        super.onPause()
    }

    override val viewPagerAdapter: FragmentPagerAdapter by lazy {
        AccountLoginProcessFragmentPagerAdapter(childFragmentManager, viewModel!!)
    }

    override fun navigationButtonClickedOnTheLastPage() {
        try {
            tryToSendLoginData()

        } catch (e: Exception) {
            // TODO: Popup implementation
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
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
                        FirebaseUser.signIn(viewModel.email.get()!!, viewModel.password.get()!!, signInUpCompletionCallback)
                    }

                    SignType.SIGN_UP -> {

                        val userConfig = FirebaseUser.UserLoginConfig(viewModel.email.get()!!,
                            viewModel.password.get()!!,
                            viewModel.name.get()!!,
                            viewModel.team.get()!!,
                            viewModel.level.get()!!.toInt())

                        FirebaseUser.signUp(userConfig, signInUpCompletionCallback)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "could not sending login data! exception: ${e.message}")
            throw Exception(App.geString(R.string.exceptions_login_missing_data))
        }
    }

    private val signInUpCompletionCallback = object: (Boolean, String?) -> Unit {
        override fun invoke(taskSuccessful: Boolean, exception: String?) {
            if (taskSuccessful) {
                close()
            } else {
                val message = exception ?: App.geString(R.string.authentication_state_authentication_failed)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun close() {
        activity?.let { SystemUtils.hideKeyboard(it) }
        fragmentManager?.popBackStack()
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: LoginViewModel): AccountLoginProcessFragment {
            val fragment = AccountLoginProcessFragment()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}