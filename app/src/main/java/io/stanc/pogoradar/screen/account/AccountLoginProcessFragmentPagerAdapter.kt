package io.stanc.pogoradar.screen.account

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.viewmodel.LoginViewModel.SignType

class AccountLoginProcessFragmentPagerAdapter(fragmentManager: FragmentManager, private val signType: SignType): FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> AccountLoginProcessPageFragment0()
            1 -> AccountLoginProcessPageFragment1()
            2 -> AccountLoginProcessPageFragment2()
            3 -> AccountLoginProcessPageFragment3()
            4 -> AccountLoginProcessPageFragment4()
            else -> throw Exception("unsupported position ($position) in AccountLoginProcessFragmentPagerAdapter!")
        }
    }

    override fun getCount(): Int {
        return when(signType) {
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