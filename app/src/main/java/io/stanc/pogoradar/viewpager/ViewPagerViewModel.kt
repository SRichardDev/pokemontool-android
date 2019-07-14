package io.stanc.pogoradar.viewpager

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class ViewPagerViewModel: ViewModel() {

    private var currentPage: Int = 0
    private val pagesValid = mutableMapOf<Int, Boolean>()

    val viewPagerButtonText = ObservableField<String>()
    val viewPagerButtonEnabled = ObservableField<Boolean>(true)

    fun reset() {
        currentPage = 0
        pagesValid.clear()
        viewPagerButtonText.set(null)
        viewPagerButtonEnabled.set(true)
    }

    fun onPageValidationChanged(page: Int, isValid: Boolean) {
        Log.d("ViewPagerViewModel", "Debug:: onPageValidationChanged(page: $page, isValid: $isValid), currentPage: $currentPage")
        pagesValid[page] = isValid
        pagesValid.keys.find { it == currentPage }?.let {
            viewPagerButtonEnabled.set(pagesValid[it])
        }
    }

    fun onPageChanged(page: Int) {
        Log.d("ViewPagerViewModel", "Debug:: onPageChanged(page: $page)")
        currentPage = page
    }
}