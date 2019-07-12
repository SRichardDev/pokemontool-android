package io.stanc.pogoradar.screen.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.SystemUtils
import io.stanc.pogoradar.viewmodel.LoginViewModel
import io.stanc.pogoradar.viewmodel.LoginViewModel.SignType
import io.stanc.pogoradar.viewmodel.ViewModelFactory
import io.stanc.pogoradar.viewpager.ViewPagerFragment

class AccountLoginProcessFragment: ViewPagerFragment() {
    private val TAG = this::class.java.name

    private var viewModel = ViewModelFactory.getViewModel(LoginViewModel::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "Debug:: onCreateView(AccountLoginProcessFragment)")

        Log.d(TAG, "Debug:: onCreateView(AccountLoginProcessFragment) viewModel: $viewModel, signType: ${viewModel?.signType?.get()?.name}")

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Debug:: AccountLoginProcessFragment onViewCreated()")
        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Debug:: AccountLoginProcessFragment onResume()")
    }

    override fun onPause() {
        Log.d(TAG, "Debug:: AccountLoginProcessFragment onPause()")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "Debug:: onDestroyView(AccountLoginProcessFragment)")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "Debug:: AccountLoginProcessFragment onDestroy()")
        AppbarManager.setTitle(getString(R.string.default_app_title))
        super.onDestroy()
    }

    override val viewPagerAdapter: FragmentPagerAdapter by lazy {
        Log.d(TAG, "Debug:: AccountLoginProcessFragment viewPagerAdapter by lazy, viewModel: $viewModel")
        AccountLoginProcessFragmentPagerAdapter(childFragmentManager, viewModel?.signType?.get()!!)
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
        Log.d(TAG, "Debug:: AccountLoginProcessFragment onPageChanged($position)")
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
        Log.d(TAG, "Debug:: AccountLoginProcessFragment close()")
        activity?.let { SystemUtils.hideKeyboard(it) }
        fragmentManager?.popBackStack()
    }
}