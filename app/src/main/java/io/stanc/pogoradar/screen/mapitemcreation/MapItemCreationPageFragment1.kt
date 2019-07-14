package io.stanc.pogoradar.screen.mapitemcreation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentMapItemCreation1Binding
import io.stanc.pogoradar.utils.addOnPropertyChanged
import io.stanc.pogoradar.viewmodel.MapItemViewModel
import io.stanc.pogoradar.viewpager.ViewPagerViewModel

class MapItemCreationPageFragment1: Fragment() {

    private val TAG = javaClass.name

    private var viewModel: MapItemViewModel? = null
    private var viewPagerViewModel: ViewPagerViewModel? = null

    private var onNameChangeCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMapItemCreation1Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MapItemViewModel::class.java)
            viewPagerViewModel = ViewModelProviders.of(it).get(ViewPagerViewModel::class.java)

            onNameChangeCallback = viewModel?.name?.addOnPropertyChanged {
                validateInput(it.get())
            }
        }

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        validateInput(viewModel?.name?.get())
    }

    override fun onDestroyView() {
        onNameChangeCallback?.let { viewModel?.name?.removeOnPropertyChangedCallback(it) }
        super.onDestroyView()
    }

    private fun validateInput(name: String?) {

        if (name?.isNotEmpty() == true) {
            viewPagerViewModel?.onPageValidationChanged(page = 1, isValid = true)
        } else {
            viewPagerViewModel?.onPageValidationChanged(page = 1, isValid = false)
        }
    }
}