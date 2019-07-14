package io.stanc.pogoradar.subscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentMapItemCreation3Binding
import io.stanc.pogoradar.map.ClusterArenaRenderer
import io.stanc.pogoradar.map.ClusterPokestopRenderer
import io.stanc.pogoradar.screen.MapItemCreationFragmentPagerAdapter
import io.stanc.pogoradar.utils.IconFactory
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.viewmodel.MapItemViewModel
import java.lang.ref.WeakReference

class MapItemCreationPageFragment2: Fragment() {

    private var viewModel: MapItemViewModel? = null
    private var mapFragment: BaseMapFragment? = null
    private var mapItemMarker: Marker? = null
        set(value) {
            field?.remove()
            field = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMapItemCreation3Binding.inflate(inflater, container, false)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MapItemViewModel::class.java)
        }
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

        mapFragment = childFragmentManager.findFragmentById(R.id.map_item_mapview) as BaseMapFragment
        mapFragment?.enableMyLocationPOI(enabled = false)
        viewModel?.position?.get()?.let { mapFragment?.updateCameraPosition(it, ZoomLevel.STREET) }
        mapFragment?.setDelegate(object : BaseMapFragment.MapDelegate {

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
}