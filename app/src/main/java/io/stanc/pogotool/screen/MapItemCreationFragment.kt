package io.stanc.pogotool.screen

import android.util.Log
import android.widget.Toast
import androidx.databinding.Observable
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.appbar.AppbarManager
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseUser
import io.stanc.pogotool.firebase.node.FirebaseArena
import io.stanc.pogotool.firebase.node.FirebasePokestop
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.SystemUtils
import io.stanc.pogotool.viewmodel.MapItemViewModel
import io.stanc.pogotool.viewmodel.MapItemViewModel.Type
import io.stanc.pogotool.viewpager.ViewPagerFragment


class MapItemCreationFragment: ViewPagerFragment() {

    private val TAG = javaClass.name

    private val firebase = FirebaseDatabase()
    private val viewModel = MapItemViewModel()

    private var position: LatLng? = null
        set(value) {
            viewModel.position.set(value)
            field = value
        }

    private val onTypeChangeCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            viewModel.type.get()?.let { updateTitle(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.type.addOnPropertyChangedCallback(onTypeChangeCallback)
    }

    override fun onPause() {
        viewModel.type.removeOnPropertyChangedCallback(onTypeChangeCallback)
        super.onPause()
    }


    override val viewPagerAdapter: FragmentPagerAdapter by lazy {
        MapItemCreationFragmentPagerAdapter(childFragmentManager, viewModel)
    }

    override fun navigationButtonClickedOnTheLastPage() {
        try {
            tryToSendMapItem()
            close()

        } catch (e: Exception) {
            // TODO: Popup implementation
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPageChanged(position: Int) {
        activity?.let { SystemUtils.hideKeyboard(it) }
    }

    private fun updateTitle(type: MapItemViewModel.Type) {
        when (type) {
            MapItemViewModel.Type.Arena -> AppbarManager.setTitle(getString(R.string.map_title_arena))
            MapItemViewModel.Type.Pokestop -> AppbarManager.setTitle(getString(R.string.map_title_pokestop))
        }
    }

    @Throws(Exception::class)
    private fun tryToSendMapItem() {

        FirebaseUser.userData?.id?.let { userId ->

            viewModel.position.get()?.let {
                val geoHash = GeoHash(it.latitude, it.longitude)

                when(viewModel.type.get()) {
                    Type.Arena -> tryToSendArena(geoHash, userId)
                    Type.Pokestop -> tryToSendPokestop(geoHash, userId)
                    else -> {
                        val text = App.geString(R.string.exceptions_send_map_item_invalid_type)
                        Log.e(TAG, "$text viewModel.type.get(): ${viewModel.type.get()}")
                        throw Exception(text)
                    }
                }

            } ?: kotlin.run {

                val text = App.geString(R.string.exceptions_send_map_item_missing_position)
                Log.e(TAG, "$text viewModel.position.get(): ${viewModel.position.get()}")
                throw Exception(text)
            }

        } ?: kotlin.run {

            val text = App.geString(R.string.exceptions_send_map_item_missing_user_id)
            Log.e(TAG, "$text FirebaseUser.userData: ${FirebaseUser.userData}")
            throw Exception(text)
        }
    }

    @Throws(Exception::class)
    private fun tryToSendArena(geoHash: GeoHash, userId: String) {

        viewModel.name.get()?.let { name ->

            val isEX = viewModel.isEx.get() ?: kotlin.run { false }
            val arena = FirebaseArena.new(name, geoHash, userId, isEX)
            firebase.pushArena(arena)

        } ?: kotlin.run {

            val text = App.geString(R.string.exceptions_send_map_item_missing_arena_name)
            Log.e(TAG, "$text viewModel.name.get(): ${viewModel.name.get()}")
            throw Exception(text)
        }
    }

    @Throws(Exception::class)
    private fun tryToSendPokestop(geoHash: GeoHash, userId: String) {

        viewModel.name.get()?.let { name ->

            val pokestop = FirebasePokestop.new(name, geoHash, userId)
            firebase.pushPokestop(pokestop)

        } ?: kotlin.run {

            val text = App.geString(R.string.exceptions_send_map_item_missing_pokestop_name)
            Log.e(TAG, "$text viewModel.name.get(): ${viewModel.name.get()}")
            throw Exception(text)
        }
    }

    private fun close() {
        fragmentManager?.popBackStack()
    }


    companion object {

        fun newInstance(latLng: LatLng): MapItemCreationFragment {
            val fragment = MapItemCreationFragment()
            fragment.position = latLng

            return fragment
        }
    }
}