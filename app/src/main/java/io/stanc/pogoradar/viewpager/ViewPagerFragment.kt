package io.stanc.pogoradar.viewpager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.LayoutViewpagerFlowBinding

abstract class ViewPagerFragment: Fragment() {

    private val TAG = javaClass.name

    protected var viewPager: ViewPager? = null
    protected abstract val viewPagerAdapter: FragmentPagerAdapter
    protected abstract fun navigationButtonClickedOnTheLastPage()
    protected abstract fun onPageChanged(position: Int)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutViewpagerFlowBinding.inflate(inflater, container, false)
        binding.viewModel = this
        Log.d(TAG, "Debug:: onCreateView(ViewPagerFragment)")


        binding.root.findViewById<ViewPager>(R.id.viewpager)?.let { viewpager ->

            viewpager.adapter = viewPagerAdapter
            viewpager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {

                override fun onPageScrollStateChanged(p0: Int) {}
                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

                override fun onPageSelected(position: Int) {
                    onPageChanged(position)
                    updateViewPagerButtonText(position)
                }

            })

            this.viewPager = viewpager

        } ?: run {
            Log.e(TAG, "could not find viewPager, therefore could not create viewPagerFragment")
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Debug:: onResume(ViewPagerFragment), childFragmentManager(${childFragmentManager.fragments.size}): ${childFragmentManager.fragments}, fragmentManager(${fragmentManager?.fragments?.size}): ${fragmentManager?.fragments})")
        Log.d(TAG, "Debug:: onResume(ViewPagerFragment) viewPager?.currentItem: ${viewPager?.currentItem}")
        viewPager?.currentItem?.let { updateViewPagerButtonText(it) }
    }

    override fun onPause() {
        Log.d(TAG, "Debug:: onPause(ViewPagerFragment) viewPager?.currentItem: ${viewPager?.currentItem}")
        super.onPause()
    }

    override fun onDestroyView() {
        Log.d(TAG, "Debug:: onDestroyView(ViewPagerFragment)")
        super.onDestroyView()
    }

    /**
     * layout interface
     */

    val viewPagerButtonText = ObservableField<String>()

    fun onButtonClicked(view: View) {

        viewPager?.let { viewpager ->

            Log.d(TAG, "Debug:: onButtonClicked(ViewPagerFragment) viewPager.currentItem: ${viewpager.currentItem}")
            if (viewpager.currentItem == viewpager.adapter?.count?.minus(1)) {
                navigationButtonClickedOnTheLastPage()
            } else {
                viewpager.setCurrentItem(viewpager.currentItem + 1, true)
            }
        } ?: run {
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