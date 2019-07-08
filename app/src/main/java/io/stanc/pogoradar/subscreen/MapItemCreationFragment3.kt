package io.stanc.pogoradar.subscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.stanc.pogoradar.R
import io.stanc.pogoradar.map.ClusterArenaRenderer
import io.stanc.pogoradar.map.ClusterPokestopRenderer
import io.stanc.pogoradar.utils.IconFactory
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.viewmodel.MapItemViewModel

class MapItemCreationFragment3: Fragment() {

    private var viewModel: MapItemViewModel? = null
    private var mapFragment: MapFragment? = null
    private var mapItemMarker: Marker? = null
        set(value) {
            field?.remove()
            field = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<io.stanc.pogoradar.databinding.FragmentMapItemCreation3Binding>(inflater, R.layout.fragment_map_item_creation_3, container, false)
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupMapFragment()
    }

    override fun onPause() {
        mapFragment?.stopAnimation()
        super.onPause()
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_item_mapview) as MapFragment
        mapFragment?.enableMyLocationPOI(enabled = false)
        viewModel?.position?.get()?.let { mapFragment?.updateCameraPosition(it, ZoomLevel.STREET) }
        mapFragment?.setDelegate(object : MapFragment.MapDelegate {

            override fun onMapReady(googleMap: GoogleMap) {
                mapItemMarker = addMarker()
            }

            override fun onCameraStartAnimationFinished() {
                viewModel?.position?.get()?.let { mapFragment?.startAnimation(it) }
            }
        })
    }

    private fun addMarker(): Marker? {

        return Kotlin.safeLet(context, viewModel?.type?.get(), viewModel?.position?.get()) { context, mapItemType, position->

            when(mapItemType) {
                MapItemViewModel.Type.Arena ->  {
                    val isArenaEx = viewModel?.isEx?.get()?: run { false }
                    mapFragment?.addMarker(ClusterArenaRenderer.arenaMarkerOptions(context, isArenaEx, IconFactory.SizeMod.BIG).position(position))
                }
                MapItemViewModel.Type.Pokestop ->  mapFragment?.addMarker(ClusterPokestopRenderer.pokestopMarkerOptions(context, IconFactory.SizeMod.BIG).position(position))
            }

        } ?: run { null }
    }

    companion object {

        private val TAG = javaClass.name

        fun newInstance(viewModel: MapItemViewModel): MapItemCreationFragment3 {
            val fragment = MapItemCreationFragment3()
            fragment.viewModel = viewModel
            return fragment
        }
    }
}