package io.stanc.pogoradar.screen

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogoradar.subscreen.MapItemCreationFragment1
import io.stanc.pogoradar.subscreen.MapItemCreationFragment2
import io.stanc.pogoradar.subscreen.MapItemCreationFragment3
import io.stanc.pogoradar.viewmodel.MapItemViewModel

class MapItemCreationFragmentPagerAdapter(fragmentManager: FragmentManager, private val mapItemViewModel: MapItemViewModel): FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val NUM_PAGES = 3

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> MapItemCreationFragment1.newInstance(mapItemViewModel)
            1 -> MapItemCreationFragment2.newInstance(mapItemViewModel)
            2 -> MapItemCreationFragment3.newInstance(mapItemViewModel)
            else -> throw Exception("unsupported position ($position) in MapItemCreationFragmentPagerAdapter!")
        }
    }

    override fun getCount() = NUM_PAGES

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Position"
            1 -> "Name"
            2 -> "Prüfen & Senden"
            else -> null
        }
    }
}