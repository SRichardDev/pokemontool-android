package io.stanc.pogoradar.subscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.stanc.pogoradar.R
import io.stanc.pogoradar.viewmodel.MapItemViewModel

class MapItemCreationFragment2: Fragment() {

    private var viewModel: MapItemViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<io.stanc.pogoradar.databinding.FragmentMapItemCreation2Binding>(inflater, R.layout.fragment_map_item_creation_2, container, false)
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