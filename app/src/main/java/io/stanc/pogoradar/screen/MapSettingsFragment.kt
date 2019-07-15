package io.stanc.pogoradar.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.App
import io.stanc.pogoradar.AppSettings
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.databinding.FragmentMapSettingsBinding

class MapSettingsFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMapSettingsBinding.inflate(inflater, container, false)
        binding.settings = AppSettings
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppbarManager.setTitle(App.geString(R.string.map_settings_title))
    }

    override fun onPause() {
        AppbarManager.reset()
        super.onPause()
    }
}