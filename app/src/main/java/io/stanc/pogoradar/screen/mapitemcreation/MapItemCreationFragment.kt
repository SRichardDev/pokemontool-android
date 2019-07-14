package io.stanc.pogoradar.screen.mapitemcreation

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.Observable
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.model.LatLng
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.appbar.AppbarManager
import io.stanc.pogoradar.firebase.FirebaseDatabase
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseArena
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.ParcelableDataFragment
import io.stanc.pogoradar.utils.SystemUtils
import io.stanc.pogoradar.viewmodel.MapItemViewModel
import io.stanc.pogoradar.viewmodel.MapItemViewModel.Type
import io.stanc.pogoradar.viewpager.ViewPagerFragment


class MapItemCreationFragment: ViewPagerFragment() {

    private val TAG = javaClass.name

    private val firebase = FirebaseDatabase()
    private var viewModel: MapItemViewModel? = null

    private val onTypeChangeCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            viewModel?.type?.get()?.let { updateTitle(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<LatLng>(ParcelableDataFragment.PARCELABLE_EXTRA_DATA_OBJECT)?.let { position ->
            activity?.let {
                viewModel = ViewModelProviders.of(it).get(MapItemViewModel::class.java)
                viewModel?.reset()
                viewModel?.position?.set(position)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.type?.get()?.let { updateTitle(it) }
        viewModel?.type?.addOnPropertyChangedCallback(onTypeChangeCallback)
    }

    override fun onPause() {
        viewModel?.type?.removeOnPropertyChangedCallback(onTypeChangeCallback)
        AppbarManager.reset()
        super.onPause()
    }


    override val viewPagerAdapter: FragmentPagerAdapter by lazy {
        MapItemCreationFragmentPagerAdapter(childFragmentManager)
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

    private fun updateTitle(type: Type) {
        when (type) {
            Type.Arena -> AppbarManager.setTitle(getString(R.string.map_title_arena))
            Type.Pokestop -> AppbarManager.setTitle(getString(R.string.map_title_pokestop))
        }
    }

    @Throws(Exception::class)
    private fun tryToSendMapItem() {

        FirebaseUser.userData?.name?.let { userName ->

            viewModel?.position?.get()?.let {
                val geoHash = GeoHash(it.latitude, it.longitude)

                when(viewModel?.type?.get()) {
                    Type.Arena -> tryToSendArena(geoHash, userName)
                    Type.Pokestop -> tryToSendPokestop(geoHash, userName)
                    else -> {
                        val text = App.geString(R.string.exceptions_send_map_item_invalid_type)
                        Log.e(TAG, "$text viewModel?.type?.get(): ${viewModel?.type?.get()}")
                        throw Exception(text)
                    }
                }

            } ?: run {

                val text = App.geString(R.string.exceptions_send_map_item_missing_position)
                Log.e(TAG, "$text viewModel?.position.get(): ${viewModel?.position?.get()}")
                throw Exception(text)
            }

        } ?: run {

            val text = App.geString(R.string.exceptions_send_map_item_missing_user_id)
            Log.e(TAG, "$text FirebaseUser.userData: ${FirebaseUser.userData}")
            throw Exception(text)
        }
    }

    @Throws(Exception::class)
    private fun tryToSendArena(geoHash: GeoHash, user: String) {

        viewModel?.name?.get()?.let { name ->

            val isEX = viewModel?.isEx?.get() ?: run { false }
            val arena = FirebaseArena.new(name, geoHash, user, isEX)
            firebase.pushArena(arena)

        } ?: run {

            val text = App.geString(R.string.exceptions_send_map_item_missing_arena_name)
            Log.e(TAG, "$text viewModel?.name.get(): ${viewModel?.name?.get()}")
            throw Exception(text)
        }
    }

    @Throws(Exception::class)
    private fun tryToSendPokestop(geoHash: GeoHash, user: String) {

        viewModel?.name?.get()?.let { name ->

            val pokestop = FirebasePokestop.new(name, geoHash, user)
            firebase.pushPokestop(pokestop)

        } ?: run {

            val text = App.geString(R.string.exceptions_send_map_item_missing_pokestop_name)
            Log.e(TAG, "$text viewModel?.name.get(): ${viewModel?.name?.get()}")
            throw Exception(text)
        }
    }

    private fun close() {
        fragmentManager?.popBackStack()
    }
}