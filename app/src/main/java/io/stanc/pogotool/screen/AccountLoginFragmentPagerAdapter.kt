package io.stanc.pogotool.screen

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.subscreen.*
import io.stanc.pogotool.viewmodel.AccountViewModel

class AccountLoginFragmentPagerAdapter(fragmentManager: FragmentManager, private val accountViewModel: AccountViewModel): FragmentPagerAdapter(fragmentManager) {

    //TODO: two ways for login/signup
    private val NUM_PAGES = 5

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> AccountLoginFragment1.newInstance(accountViewModel)
            1 -> AccountLoginFragment2.newInstance(accountViewModel)
            2 -> AccountLoginFragment3.newInstance(accountViewModel)
            3 -> AccountLoginFragment4.newInstance(accountViewModel)
            4 -> AccountLoginFragment5.newInstance(accountViewModel)
            else -> null
        }
    }

    override fun getCount() = NUM_PAGES

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