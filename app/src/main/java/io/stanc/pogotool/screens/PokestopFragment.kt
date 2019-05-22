package io.stanc.pogotool.screens

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.databinding.FragmentPokestopBinding
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.map.ClusterPokestopRenderer
import io.stanc.pogotool.map.MapFragment
import io.stanc.pogotool.utils.KotlinUtils
import java.util.*

class PokestopFragment: Fragment() {

    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var arenaMarker: Marker? = null
    private var position: LatLng? = null

    private var pokestop: FirebasePokestop? = null
        set(value) {
            field = value
            updateLayout()

            value?.let {
                viewModel?.updateData(value) ?: kotlin.run {
                    viewModel = RaidViewModel(it)
                }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPokestopBinding>(inflater, R.layout.fragment_pokestop, container, false)
//        binding.viewmodel = viewModel
//        viewBinding = binding

        setupMapFragment()
        updateLayout(binding.root)

        return binding.root
    }

    private fun updateLayout(rootLayout: View) {

        pokestop?.let { pokestop ->

            rootLayout.findViewById<TextView>(R.id.map_item_infos_textview_coordinates)?.let { textView ->
                val latitude = pokestop.geoHash.toLocation().latitude.toString()
                val longitude = pokestop.geoHash.toLocation().longitude.toString()
                textView.text = getString(R.string.coordinates_format, latitude, longitude)
            }

            rootLayout.findViewById<TextView>(R.id.map_item_infos_textview_added_from_user)?.let { textView ->
                textView.text = pokestop.submitter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pokestop?.let { AppbarManager.setTitle(it.name) }
//        arena?.let { firebase.addObserver(arenaObserver, it) }
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_item_mapview) as MapFragment
        position?.let { mapFragment?.setNextStartPosition(it.latitude, it.longitude) }
        mapFragment?.enableMyLocationPOI(enabled = false)
        mapFragment?.setDelegate(object : MapFragment.MapDelegate {

            override fun onCameraStartAnimationFinished() {
                position?.let { mapFragment?.startAnimation(it) }
            }

            override fun onMapReady(googleMap: GoogleMap) {
                map = googleMap
                addMarker()
            }
        })
    }

    private fun addMarker(): Marker? {

        return KotlinUtils.safeLet(context, position) { _context, _position->
            map?.addMarker(ClusterPokestopRenderer.pokestopMarkerOptions(_context).position(_position))
        } ?: kotlin.run { null }
    }

    companion object {

        fun newInstance(latLng: LatLng): PokestopFragment {
            val fragment = PokestopFragment()
            fragment.position = latLng
            return fragment
        }
    }
}