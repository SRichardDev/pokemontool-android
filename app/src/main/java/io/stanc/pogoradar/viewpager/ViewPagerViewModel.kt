package io.stanc.pogoradar.viewpager

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
        pagesValid[page] = isValid
        checkButtonEnable()
    }

    fun onPageChanged(page: Int) {
        currentPage = page
        checkButtonEnable()
    }

    private fun checkButtonEnable() {
        pagesValid.keys.find { it == currentPage }?.let {
            viewPagerButtonEnabled.set(pagesValid[it])
        }
    }
}