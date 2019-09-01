package io.stanc.pogoradar.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.MapFilterSettings
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.databinding.FragmentMapSettingsBinding

class MapSettingsFragment: Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMapSettingsBinding.inflate(inflater, container, false)
        binding.settings = MapFilterSettings
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        AppbarManager.setTitle(getString(R.string.map_settings_title))
    }
}