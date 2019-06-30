package io.stanc.pogotool.screen

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.viewmodel.LoginViewModel
import io.stanc.pogotool.viewmodel.LoginViewModel.SignType
import io.stanc.pogotool.viewpager.ViewPagerFragment

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
        Log.i(TAG, "Debug:: navigationButtonClickedOnTheLastPage() email: ${viewModel?.email?.get()}, password: ${viewModel?.password?.get()}")
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

                        val userConfig = FirebaseUser.UserConfig(viewModel.email.get()!!,
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