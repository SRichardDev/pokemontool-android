package io.stanc.pogoradar.viewmodel.pokestop

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableField
import io.stanc.pogoradar.firebase.node.FirebasePokestop
import io.stanc.pogoradar.AppSettings
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.viewmodel.MapItemInfoViewModel

class PokestopViewModel: ViewModel() {

    var pokestop: FirebasePokestop? = null
    val isPokestopVisibleOnMap = ObservableField<Boolean>()
    val name = ObservableField<String>()
    val geoHash = ObservableField<GeoHash>()
    val mapItemInfoViewModel = MapItemInfoViewModel()

    fun updateData(pokestop: FirebasePokestop?, context: Context) {
        this.pokestop = pokestop

        pokestop?.let {

            var visible = AppSettings.enablePokestops.get() == true
            if (visible) {
                val viewModel = QuestViewModel.new(pokestop, context)
                visible = !(viewModel.questExists.get() == false && AppSettings.justQuestPokestops.get() == true)
            }
            isPokestopVisibleOnMap.set(visible)

            name.set(pokestop.name)
            geoHash.set(pokestop.geoHash)

            mapItemInfoViewModel.updateData(pokestop.submitter, pokestop.geoHash)

        } ?: run {
            reset()
        }
    }

    fun reset() {
        pokestop = null
        isPokestopVisibleOnMap.set(false)
        name.set(null)
        geoHash.set(null)
        mapItemInfoViewModel.reset()
    }

    companion object {

        fun new(pokestop: FirebasePokestop, context: Context): PokestopViewModel {
            val viewModel = PokestopViewModel()
            viewModel.updateData(pokestop, context)
            return viewModel
        }
    }
}