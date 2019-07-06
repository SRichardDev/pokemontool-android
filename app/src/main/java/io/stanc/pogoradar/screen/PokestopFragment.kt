package io.stanc.pogoradar.screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.databinding.FragmentPokestopBinding
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseNodeObserverManager
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.map.ClusterPokestopRenderer
import io.stanc.pogoradar.subscreen.MapFragment
import io.stanc.pogoradar.utils.Kotlin
import io.stanc.pogoradar.utils.ShowFragmentManager
import io.stanc.pogoradar.viewmodel.QuestViewModel


class PokestopFragment: Fragment() {
    private val TAG = javaClass.name

    private var firebase: FirebaseDatabase = FirebaseDatabase()
    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var position: LatLng? = null
    private var viewModel: QuestViewModel? = null
    private var viewBinding: FragmentPokestopBinding? = null

    private var pokestop: FirebasePokestop? = null
        set(value) {
            field = value

            updateViewModel(value)
            updateLayout()
        }

    private val pokestopObserver = object: FirebaseNodeObserverManager.Observer<FirebasePokestop> {

        override fun onItemChanged(item: FirebasePokestop) {
            pokestop = item
        }

        override fun onItemRemoved(itemId: String) {
            pokestop = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPokestopBinding>(inflater, R.layout.fragment_pokestop, container, false)
        binding.viewmodel = viewModel
        viewBinding = binding

        setupMapFragment()
        updateLayout()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        pokestop?.let { AppbarManager.setTitle(it.name) }
        pokestop?.let { firebase.addObserver(pokestopObserver, it) }
    }

    override fun onPause() {
        pokestop?.let { firebase.removeObserver(pokestopObserver, it) }
        AppbarManager.setTitle(getString(R.string.default_app_title))
        super.onPause()
    }

    private fun updateViewModel(pokestop: FirebasePokestop?) {
        pokestop?.let {
            viewModel?.updateData(it) ?: run {
                viewModel = QuestViewModel(it)
            }
            position = LatLng(it.geoHash.toLocation().latitude, it.geoHash.toLocation().longitude)
        } ?: run {
            viewModel = null
        }
    }

    private fun updateLayout() {
        Log.d(TAG, "Debug:: updateLayout(), pokestop: $pokestop, viewBinding?.root: ${viewBinding?.root}")

        pokestop?.let { pokestop ->

            viewBinding?.root?.apply {

                this.findViewById<Button>(R.id.pokestop_button_new_quest)?.let {
                    it.setOnClickListener {
                        showQuestFragment(pokestop)
                    }
                }

                this.findViewById<TextView>(R.id.map_item_infos_textview_coordinates)?.let { textView ->
                    val latitude = pokestop.geoHash.toLocation().latitude.toString()
                    val longitude = pokestop.geoHash.toLocation().longitude.toString()
                    textView.text = getString(R.string.coordinates_format, latitude, longitude)
                }

                this.findViewById<TextView>(R.id.map_item_infos_textview_added_from_user)?.let { textView ->
                    textView.text = pokestop.submitter
                }

                this.findViewById<ImageView>(R.id.pokestop_layout_quest_image)?.let { imageView ->
                    viewModel?.imageDrawable(context)?.let { imageView.setImageDrawable(it) }
                }
            }
        }
    }

    private fun setupMapFragment() {

        mapFragment = childFragmentManager.findFragmentById(R.id.map_item_mapview) as MapFragment
        mapFragment?.enableMyLocationPOI(enabled = false)
        position?.let { mapFragment?.updateCameraPosition(it) }
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

        return Kotlin.safeLet(context, position) { _context, _position->
            map?.addMarker(ClusterPokestopRenderer.pokestopMarkerOptions(_context).position(_position))
        } ?: run { null }
    }

    private fun showQuestFragment(pokestop: FirebasePokestop) {
        val fragment = QuestFragment.newInstance(pokestop)
        ShowFragmentManager.showFragment(fragment, fragmentManager, R.id.fragment_map_layout)
    }

    companion object {

        fun newInstance(pokestop: FirebasePokestop): PokestopFragment {
            val fragment = PokestopFragment()
            fragment.pokestop = pokestop
            return fragment
        }
    }
}