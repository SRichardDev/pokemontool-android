package io.stanc.pogotool.screen

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.subscreen.*
import io.stanc.pogotool.viewmodel.LoginViewModel
import io.stanc.pogotool.viewmodel.LoginViewModel.SignType

class AccountLoginProcessFragmentPagerAdapter(fragmentManager: FragmentManager, private val loginViewModel: LoginViewModel): FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> AccountLoginFragment1.newInstance(loginViewModel)
            1 -> AccountLoginFragment2.newInstance(loginViewModel)
            2 -> AccountLoginFragment3.newInstance(loginViewModel)
            3 -> AccountLoginFragment4.newInstance(loginViewModel)
            4 -> AccountLoginFragment5.newInstance(loginViewModel)
            else -> throw Exception("unsupported position ($position) in AccountLoginProcessFragmentPagerAdapter!")
        }
    }

    override fun getCount(): Int {
        return when(loginViewModel.signType.get()) {
            SignType.SIGN_IN -> 2
            SignType.SIGN_UP -> 5
            else -> 0
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> App.geString(R.string.authentication_login_screen_title_1)
            1 -> App.geString(R.string.authentication_login_screen_title_2)
            2 -> App.geString(R.string.authentication_login_screen_title_3)
            3 -> App.geString(R.string.authentication_login_screen_title_4)
            4 -> App.geString(R.string.authentication_login_screen_title_5)
            else -> null
        }
    }
}