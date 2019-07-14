package io.stanc.pogoradar.screen.mapitemcreation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R

class MapItemCreationFragmentPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val NUM_PAGES = 3

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> MapItemCreationPageFragment0()
            1 -> MapItemCreationPageFragment1()
            2 -> MapItemCreationPageFragment2()
            else -> throw Exception("unsupported position ($position) in MapItemCreationFragmentPagerAdapter!")
        }
    }

    override fun getCount() = NUM_PAGES

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> App.geString(R.string.map_map_item_creation_page_title_1)
            1 -> App.geString(R.string.map_map_item_creation_page_title_2)
            2 -> App.geString(R.string.map_map_item_creation_page_title_3)
            else -> null
        }
    }
}