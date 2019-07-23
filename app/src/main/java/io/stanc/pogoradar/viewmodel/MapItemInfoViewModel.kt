package io.stanc.pogoradar.viewmodel

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.geohash.GeoHash

class MapItemInfoViewModel: ViewModel() {

    val submitter = ObservableField<String>()
    val coordinates = ObservableField<String>()

    fun updateData(submitter: String, geoHash: GeoHash) {
        this.submitter.set(submitter)
        val latitude = geoHash.toLocation().latitude.toString()
        val longitude = geoHash.toLocation().longitude.toString()
        coordinates.set(App.geString(R.string.coordinates_format, latitude, longitude))
    }

    fun reset() {
        submitter.set(null)
        coordinates.set(null)
    }
}