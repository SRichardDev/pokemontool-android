package io.stanc.pogoradar.subscreen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.stanc.pogoradar.databinding.FragmentMapItemCreation2Binding
import io.stanc.pogoradar.screen.MapItemCreationFragmentPagerAdapter
import io.stanc.pogoradar.utils.addOnPropertyChanged
import io.stanc.pogoradar.viewmodel.MapItemViewModel
import io.stanc.pogoradar.viewpager.ViewPagerViewModel
import java.lang.ref.WeakReference

class MapItemCreationPageFragment1: Fragment() {

    private val TAG = javaClass.name

    private var viewModel: MapItemViewModel? = null
    private var viewPagerViewModel: ViewPagerViewModel? = null

    private var onNameChangeCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMapItemCreation2Binding.inflate(inflater, container, false)

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

        Log.i(TAG, "Debug:: validateInput, name: $name")
        if (name?.isNotEmpty() == true) {
            viewPagerViewModel?.onPageValidationChanged(page = 1, isValid = true)
        } else {
            viewPagerViewModel?.onPageValidationChanged(page = 1, isValid = false)
        }
    }
}