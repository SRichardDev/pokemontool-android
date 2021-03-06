package io.stanc.pogoradar.viewpager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.databinding.LayoutViewpagerFlowBinding
import io.stanc.pogoradar.utils.addOnPropertyChanged

abstract class ViewPagerFragment: Fragment() {

    private val TAG = javaClass.name

    private var viewModel: ViewPagerViewModel? = null
    private var viewPager: ControlFlowViewPager? = null

    protected abstract val viewPagerAdapter: FragmentPagerAdapter
    protected abstract fun navigationButtonClickedOnTheLastPage()
    protected abstract fun onPageChanged(position: Int)

    private var viewPageButtonCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutViewpagerFlowBinding.inflate(inflater, container, false)

        setupViewModel(binding)
        setupViewPager(binding)
        setupViewPagerButton(binding)

        return binding.root
    }

    private fun setupViewModel(binding: LayoutViewpagerFlowBinding) {

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(ViewPagerViewModel::class.java)
            viewModel?.reset()
            viewPageButtonCallback = viewModel?.viewPagerButtonEnabled?.addOnPropertyChanged { buttonEnabled ->
                viewPager?.swipeEnabled = buttonEnabled.get() == true
            }
            binding.viewModel = viewModel
        }
    }

    private fun setupViewPager(binding: LayoutViewpagerFlowBinding) {

        binding.root.findViewById<ControlFlowViewPager>(R.id.viewpager)?.let { viewpager ->

            viewpager.adapter = viewPagerAdapter
            viewpager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {

                override fun onPageScrollStateChanged(p0: Int) {}
                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

                override fun onPageSelected(position: Int) {
                    onPageChanged(position)
                    updateViewPagerButtonText(position)
                    viewModel?.onPageChanged(position)
                }

            })

            this.viewPager = viewpager

        } ?: run {
            Log.e(TAG, "could not find viewPager, therefore could not create viewPagerFragment")
        }
    }

    private fun setupViewPagerButton(binding: LayoutViewpagerFlowBinding) {

        binding.root.findViewById<Button>(R.id.viewpager_button)?.setOnClickListener {
            viewPager?.let { viewpager ->

                if (viewpager.currentItem == viewpager.adapter?.count?.minus(1)) {
                    navigationButtonClickedOnTheLastPage()
                } else {
                    viewpager.setCurrentItem(viewpager.currentItem + 1, true)
                }
            } ?: run {
                Log.e(TAG, "on view pager fragment button clicked, but view pager is null!")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewPager?.currentItem?.let { updateViewPagerButtonText(it) }
    }

    override fun onDestroyView() {
        viewPageButtonCallback?.let { viewModel?.viewPagerButtonEnabled?.removeOnPropertyChangedCallback(it) }
        super.onDestroyView()
    }

    /**
     * private implementation
     */

    private fun updateViewPagerButtonText(currentPagePosition: Int) {

        viewPager?.adapter?.count?.let { pageCount ->

            if (currentPagePosition < pageCount - 1) {
                viewModel?.viewPagerButtonText?.set(App.geString(R.string.map_map_item_button_further))
            } else {
                viewModel?.viewPagerButtonText?.set(App.geString(R.string.map_map_item_button_send))
            }
        }
    }
}