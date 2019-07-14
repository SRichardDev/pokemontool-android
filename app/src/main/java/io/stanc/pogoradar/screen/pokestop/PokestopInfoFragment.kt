package io.stanc.pogoradar.screen.pokestop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.FragmentPokestopInfoBinding
import io.stanc.pogoradar.map.ClusterPokestopRenderer
import io.stanc.pogoradar.subscreen.BaseMapFragment
import io.stanc.pogoradar.subscreen.ZoomLevel
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.PokestopViewModel
import io.stanc.pogoradar.viewmodel.QuestViewModel


class PokestopInfoFragment: Fragment() {
    private val TAG = javaClass.name

    private var mapFragment: BaseMapFragment? = null
    private var map: GoogleMap? = null

    private var pokestopViewModel: PokestopViewModel? = null
    private var questViewModel: QuestViewModel? = null

    private var viewBinding: FragmentPokestopInfoBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentPokestopInfoBinding.inflate(inflater, container, false)

        activity?.let {
            pokestopViewModel = ViewModelProviders.of(it).get(PokestopViewModel::class.java)
            questViewModel = ViewModelProviders.of(it).get(QuestViewModel::class.java)
        }

        binding.questViewModel = questViewModel
        binding.pokestopViewModel = pokestopViewModel
        viewBinding = binding

        setupMapFragment()

        binding.root.findViewById<Button>(R.id.pokestop_button_new_quest)?.setOnClickListener {
            ShowFragmentManager.showFragment(QuestFragment(), fragmentManager, R.id.pokestop_layout)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        pokestopViewModel?.geoHash?.get()?.let { mapFragment?.updateCameraPosition(it, ZoomLevel.STREET) }
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_item_mapview) as BaseMapFragment
        mapFragment?.enableMyLocationPOI(enabled = false)
        pokestopViewModel?.geoHash?.get()?.let { mapFragment?.updateCameraPosition(it, ZoomLevel.STREET) }
        mapFragment?.setDelegate(object : BaseMapFragment.MapDelegate {

            override fun onCameraStartAnimationFinished() {
                pokestopViewModel?.geoHash?.get()?.toLatLng()?.let { mapFragment?.startAnimation(it) }
            }

            override fun onMapReady(googleMap: GoogleMap) {
                map = googleMap
                addMarker()
            }
        })
    }

    private fun addMarker(): Marker? {

        return Kotlin.safeLet(context, pokestopViewModel?.geoHash?.get()?.toLatLng()) { _context, _position->
            map?.addMarker(ClusterPokestopRenderer.pokestopMarkerOptions(_context).position(_position))
        } ?: run {
            null
        }
    }
}