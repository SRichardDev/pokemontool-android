package io.stanc.pogotool.subscreen

import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.stanc.pogotool.R
import io.stanc.pogotool.map.ClusterArenaRenderer
import io.stanc.pogotool.map.ClusterPokestopRenderer
import io.stanc.pogotool.utils.Kotlin
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.viewmodel.MapItemViewModel

class MapItemCreationFragment2: Fragment() {

    private var viewModel: MapItemViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<io.stanc.pogotool.databinding.FragmentMapItemCreation2Binding>(inflater, R.layout.fragment_map_item_creation_2, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: MapItemViewModel): MapItemCreationFragment2 {
            val fragment = MapItemCreationFragment2()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}