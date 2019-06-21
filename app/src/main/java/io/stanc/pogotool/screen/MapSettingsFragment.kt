package io.stanc.pogotool.screen

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentMapSettingsBinding
import io.stanc.pogotool.AppSettings

class MapSettingsFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentMapSettingsBinding>(inflater, R.layout.fragment_map_settings, container, false)

        binding.settings = AppSettings

        AppbarManager.setTitle(resources.getString(R.string.map_settings_title))

        return binding.root
    }
}