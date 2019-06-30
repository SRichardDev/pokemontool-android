package io.stanc.pogotool.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.stanc.pogotool.App
import io.stanc.pogotool.AppSettings
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentMapSettingsBinding

class MapSettingsFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentMapSettingsBinding>(inflater, R.layout.fragment_map_settings, container, false)
        binding.settings = AppSettings
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(App.geString(R.string.map_settings_title))
    }

    override fun onPause() {
        AppbarManager.setTitle(getString(R.string.default_app_title))
        super.onPause()
    }
}