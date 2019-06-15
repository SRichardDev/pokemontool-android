package io.stanc.pogotool.viewpager

import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.databinding.LayoutViewpagerBinding
import java.text.FieldPosition

abstract class ViewPagerFragment: Fragment() {

    private val TAG = javaClass.name

    protected var viewPager: ViewPager? = null
    protected abstract fun viewPagerAdapter(): FragmentPagerAdapter?
    protected abstract fun navigationButtonClickedOnTheLastPage()
    protected abstract fun onPageChanged(position: Int)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<LayoutViewpagerBinding>(inflater, R.layout.layout_viewpager, container, false)

        binding.viewModel = this

        binding.root.findViewById<ViewPager>(R.id.viewpager)?.let { viewpager ->

            viewpager.adapter = viewPagerAdapter()
            viewpager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {

                override fun onPageScrollStateChanged(p0: Int) {}
                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

                override fun onPageSelected(position: Int) {
                    onPageChanged(position)
                    updateViewPagerButtonText(position)
                }

            })

            this.viewPager = viewpager

        } ?: kotlin.run {
            Log.e(TAG, "could not find viewPager, therefore could not create viewPagerFragment")
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewPager?.currentItem?.let { updateViewPagerButtonText(it) }
    }

    /**
     * layout interface
     */

    val viewPagerButtonText = ObservableField<String>()

    fun onButtonClicked(view: View) {

        viewPager?.let { viewpager ->

            if (viewpager.currentItem == viewpager.adapter?.count?.minus(1)) {
                navigationButtonClickedOnTheLastPage()
            } else {
                viewpager.setCurrentItem(viewpager.currentItem + 1, true)
            }
        } ?: kotlin.run {
            Log.e(TAG, "on view pager fragment button clicked, but view pager is null!")
        }

    }

    /**
     * private implementation
     */

    private fun updateViewPagerButtonText(currentPagePosition: Int) {

        viewPager?.adapter?.count?.let { pageCount ->

            if (currentPagePosition < pageCount - 1) {
                viewPagerButtonText.set(App.geString(R.string.map_map_item_button_further))
            } else {
                viewPagerButtonText.set(App.geString(R.string.map_map_item_button_send))
            }
        }
    }
}