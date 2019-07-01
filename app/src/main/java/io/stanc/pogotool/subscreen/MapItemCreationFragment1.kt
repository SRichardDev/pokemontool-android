package io.stanc.pogotool.subscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.stanc.pogotool.R
import io.stanc.pogotool.map.ClusterArenaRenderer
import io.stanc.pogotool.map.ClusterPokestopRenderer
import io.stanc.pogotool.utils.IconFactory
import io.stanc.pogotool.utils.InterceptableScrollView
import io.stanc.pogotool.utils.Kotlin
import io.stanc.pogotool.viewmodel.MapItemViewModel

class MapItemCreationFragment1: Fragment() {

    private var viewModel: MapItemViewModel? = null
    private var scrollview: InterceptableScrollView? = null
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

        scrollview = binding.root.findViewById(R.id.scrollview)
            viewModel?.type?.addOnPropertyChangedCallback(onTypeChangeCallback)
        viewModel?.isEx?.addOnPropertyChangedCallback(onTypeChangeCallback)
        setupMapFragment()

        return binding.root
    }

    override fun onDestroyView() {
        viewModel?.type?.removeOnPropertyChangedCallback(onTypeChangeCallback)
        viewModel?.isEx?.removeOnPropertyChangedCallback(onTypeChangeCallback)
        mapFragment?.mapView?.let { scrollview?.removeInterceptScrollView(it) }
        super.onDestroyView()
    }

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
                mapFragment?.mapView?.let { scrollview?.addInterceptScrollView(it) }
            }
        })
    }

    private fun updateMarker() {
        mapItemMarker = addMarker()
    }

    private fun addMarker(): Marker? {

        val markerOptions = Kotlin.safeLet(context, viewModel?.type?.get()) { context, mapItemType ->

            when(mapItemType) {
                MapItemViewModel.Type.Arena ->  {
                    val isArenaEx = viewModel?.isEx?.get()?: run { false }
                    ClusterArenaRenderer.arenaMarkerOptions(context, isArenaEx, IconFactory.SizeMod.BIG)
                }
                MapItemViewModel.Type.Pokestop ->  {
                    ClusterPokestopRenderer.pokestopMarkerOptions(context, IconFactory.SizeMod.BIG)
                }
            }

        } ?: run { null }


        return markerOptions?.let { _markerOptions ->

            viewModel?.position?.get()?.let { _markerOptions.position(it) }
            _markerOptions.draggable(true)
            map?.addMarker(_markerOptions)

        } ?: run {
            null
        }
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