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
import io.stanc.pogotool.viewmodel.MapItemViewModel

class MapItemCreationFragment1: Fragment() {

    private var viewModel: MapItemViewModel? = null
    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var mapItemMarker: Marker? = null
        set(value) {
            field?.remove()
            field = value
        }

    private val onTypeChangeCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            updateMarker()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<io.stanc.pogotool.databinding.FragmentMapItemCreation1Binding>(inflater, R.layout.fragment_map_item_creation_1, container, false)
        binding.viewModel = viewModel

        viewModel?.type?.addOnPropertyChangedCallback(onTypeChangeCallback)
        viewModel?.isEx?.addOnPropertyChangedCallback(onTypeChangeCallback)
        setupMapFragment()

        return binding.root
    }

    override fun onDestroyView() {
        viewModel?.type?.removeOnPropertyChangedCallback(onTypeChangeCallback)
        viewModel?.isEx?.removeOnPropertyChangedCallback(onTypeChangeCallback)
        super.onDestroyView()
    }

    // TODO: add functionality to move marker on map
    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_item_mapview) as MapFragment
        viewModel?.position?.get()?.let { mapFragment?.setNextStartPosition(it.latitude, it.longitude) }
        mapFragment?.enableMyLocationPOI(enabled = false)
        mapFragment?.setDelegate(object : MapFragment.MapDelegate {

            override fun onCameraStartAnimationFinished() {
                viewModel?.position?.get()?.let { mapFragment?.startAnimation(it) }
            }

            override fun onMapReady(googleMap: GoogleMap) {
                map = googleMap
                updateMarker()
            }
        })
    }

    private fun updateMarker() {
        mapItemMarker = addMarker()
    }

    private fun addMarker(): Marker? {

        return Kotlin.safeLet(context, viewModel?.type?.get(), viewModel?.position?.get()) { context, mapItemType, position->

            when(mapItemType) {
                MapItemViewModel.Type.Arena ->  {
                    val isArenaEx = viewModel?.isEx?.get()?: kotlin.run { false }
                    map?.addMarker(ClusterArenaRenderer.arenaMarkerOptions(context, isArenaEx).position(position))
                }
                MapItemViewModel.Type.Pokestop ->  map?.addMarker(ClusterPokestopRenderer.pokestopMarkerOptions(context).position(position))
            }

        } ?: kotlin.run { null }
    }

    private fun close() {
        fragmentManager?.popBackStack()
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: MapItemViewModel): MapItemCreationFragment1 {
            val fragment = MapItemCreationFragment1()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}