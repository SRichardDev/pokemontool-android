package io.stanc.pogotool.screen

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import io.stanc.pogotool.subscreen.MapFragment
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.SystemUtils
import kotlinx.android.synthetic.main.fragment_map_item.*
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.map.ClusterArenaRenderer
import io.stanc.pogotool.map.ClusterPokestopRenderer
import io.stanc.pogotool.utils.Kotlin


class MapItemCreationFragment: Fragment() {

    private val TAG = javaClass.name

    private var position: LatLng? = null

    private var firebase: FirebaseDatabase = FirebaseDatabase()

    private var mapFragment: MapFragment? = null
    private var map: GoogleMap? = null
    private var arenaMarker: Marker? = null
    private var checkboxIsExArena: CheckBox? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_map_item, container, false) as ViewGroup

        setupMapFragment()
        rootView.setOnClickListener { activity?.let { SystemUtils.hideKeyboard(it) } }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map_item_edittext?.hint = getString(R.string.map_map_item_edittext, mapItemName)
        map_item_button_send?.setOnClickListener {
            Log.d(TAG, "map_item_button.clicked, map_item_edittext.hint: ${map_item_edittext.hint}, map_item_edittext.text: ${map_item_edittext.text}")
            sendMapItem()
            close()
        }
        map_item_button_cancel?.setOnClickListener {
            close()
        }

        if (mapItem == MapItem.ARENA) {

            view.findViewById<CheckBox>(R.id.map_item_checkbox_isex_arena)?.let { checkbox ->

                checkbox.visibility = View.VISIBLE
                checkbox.setOnClickListener {
                    arenaMarker?.remove()
                    addMarker(checkbox.isChecked)
                }

                checkboxIsExArena = checkbox
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mapItem == MapItem.ARENA) {
            AppbarManager.setTitle(getString(R.string.map_title_arena))
        } else if (mapItem == MapItem.POKESTOP) {
            AppbarManager.setTitle(getString(R.string.map_title_pokestop))
        }
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
                val isArenaEx = checkboxIsExArena?.isChecked ?: kotlin.run { false }
                arenaMarker = addMarker(isArenaEx)
            }
        })
    }

    private fun addMarker(isArenaEx: Boolean): Marker? {

        return Kotlin.safeLet(context, position) { _context, _position->

            when(mapItem) {
                MapItem.ARENA ->  map?.addMarker(ClusterArenaRenderer.arenaMarkerOptions(_context, isArenaEx).position(_position))
                MapItem.POKESTOP ->  map?.addMarker(ClusterPokestopRenderer.pokestopMarkerOptions(_context).position(_position))
                else -> null
            }

        } ?: kotlin.run { null }
    }

    /**
     * methods
     */

    private fun sendMapItem() {

        FirebaseUser.userData?.id?.let { userId ->
            position?.let {
                val geoHash = GeoHash(it.latitude, it.longitude)

                when(mapItem) {
                    MapItem.ARENA -> {
                        map_item_edittext?.text?.toString()?.let { name ->
                            val isEX = map_item_checkbox_isex_arena?.isChecked?:kotlin.run { false }
                            val arena = FirebaseArena.new(name, geoHash, userId, isEX)
                            Log.d(TAG, "push arena: $arena")
                            firebase.pushArena(arena)
                        }

                    }

                    MapItem.POKESTOP -> {
                        map_item_edittext?.text?.toString()?.let { name ->
                            val pokestop = FirebasePokestop("", name, geoHash, userId)
                            Log.d(TAG, "push pokestop: $pokestop")
                            firebase.pushPokestop(pokestop)
                        }
                    }

                    else -> Log.e(TAG, "Could not sending map item to server! Reason: unknown mapItem: $mapItem")
                }
            } ?: kotlin.run { Log.e(TAG, "Could not sending map item to server! Reason: position is $position") }
        } ?: kotlin.run { Log.e(TAG, "Could not sending map item to server! Reason: submitter userId is missing, userData: ${FirebaseUser.userData}") }
    }

    private fun close() {
        activity?.let { SystemUtils.hideKeyboard(it) }
        fragmentManager?.popBackStack()
    }

    companion object {

        enum class MapItem {
            ARENA,
            POKESTOP
        }

        fun newInstance(latLng: LatLng): MapItemCreationFragment {
            val fragment = MapItemCreationFragment()
            fragment.position = latLng

            return fragment
        }
    }
}