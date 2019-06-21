package io.stanc.pogotool.screen

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.stanc.pogotool.subscreen.MapItemCreationFragment1
import io.stanc.pogotool.subscreen.MapItemCreationFragment2
import io.stanc.pogotool.subscreen.MapItemCreationFragment3
import io.stanc.pogotool.viewmodel.MapItemViewModel

class MapItemCreationFragmentPagerAdapter(fragmentManager: FragmentManager, private val mapItemViewModel: MapItemViewModel): FragmentPagerAdapter(fragmentManager) {

    private val NUM_PAGES = 3

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> MapItemCreationFragment1.newInstance(mapItemViewModel)
            1 -> MapItemCreationFragment2.newInstance(mapItemViewModel)
            2 -> MapItemCreationFragment3.newInstance(mapItemViewModel)
            else -> null
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