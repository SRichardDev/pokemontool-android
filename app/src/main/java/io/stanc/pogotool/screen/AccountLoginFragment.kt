package io.stanc.pogotool.screen

import android.os.Bundle
import android.support.v4.app.FragmentPagerAdapter
import android.util.Log
import android.widget.Toast
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.viewmodel.AccountViewModel
import io.stanc.pogotool.viewpager.ViewPagerFragment

class AccountLoginFragment: ViewPagerFragment() {

    var viewModel: AccountViewModel? = null
    private val firebase = FirebaseDatabase()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppbarManager.setTitle(App.geString(R.string.authentication_app_title))
        super.onCreate(savedInstanceState)
    }

    override val viewPagerAdapter: FragmentPagerAdapter by lazy {

        viewModel?.let {

            AccountLoginFragmentPagerAdapter(childFragmentManager, it)

        } ?: kotlin.run {

            val viewModel = AccountViewModel()
            this.viewModel = viewModel
            AccountLoginFragmentPagerAdapter(childFragmentManager, viewModel)
        }
    }

    override fun navigationButtonClickedOnTheLastPage() {
        Log.i(TAG, "Debug:: navigationButtonClickedOnTheLastPage()")
        try {
            tryToSendLoginData()
            close()

        } catch (e: Exception) {
            // TODO: Popup implementation
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPageChanged(position: Int) {
        activity?.let { SystemUtils.hideKeyboard(it) }
    }

    private fun tryToSendLoginData() {
        // TODO...
    }

    private fun close() {
        Log.i(TAG, "Debug:: close()")
        fragmentManager?.popBackStack()
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: AccountViewModel): AccountLoginFragment {
            val fragment = AccountLoginFragment()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}