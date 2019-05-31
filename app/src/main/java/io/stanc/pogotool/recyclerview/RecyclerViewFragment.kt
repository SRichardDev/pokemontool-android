package io.stanc.pogotool.recyclerview

import android.content.Context
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


abstract class RecyclerViewFragment<ItemType: IdItem>: Fragment() {

    private val TAG = javaClass.name

    private var listAdapter: RecyclerViewAdapter<ItemType>? = null
    private var recyclerView: RecyclerView? = null

    enum class Orientation(val value: Int) {
        HORIZONTAL(LinearLayoutManager.HORIZONTAL),
        VERTICAL(LinearLayoutManager.VERTICAL)
    }

    /**
     * customize
     */

    @get:LayoutRes
    abstract val fragmentLayoutRes: Int
    @get:IdRes
    abstract val recyclerViewIdRes: Int

    abstract val orientation: Orientation

    abstract val initItemList: List<ItemType>

    abstract fun onCreateListAdapter(context: Context, list: List<ItemType>): RecyclerViewAdapter<ItemType>

    /**
     * interface
     */

    fun showList(itemList: List<ItemType>) {
        setupList(itemList)
    }

    fun selectedItem(): ItemType? {
        return listAdapter?.selectedItem
    }

    /**
     * setup
     */

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootLayout = inflater.inflate(fragmentLayoutRes, container, false)

        recyclerView = rootLayout.findViewById(recyclerViewIdRes)
        setupList(initItemList)

        return rootLayout
    }

    private fun setupList(list: List<ItemType>) {
        recyclerView?.let { recyclerView ->

            context?.let {
                val adapter = onCreateListAdapter(it, list)

                val layoutManager = LinearLayoutManager(it)
                layoutManager.orientation = orientation.value
                recyclerView.layoutManager = layoutManager

                val dividerItemDecoration = DividerItemDecoration(
                    recyclerView.context,
                    layoutManager.orientation
                )
                recyclerView.addItemDecoration(dividerItemDecoration)

                recyclerView.adapter = adapter
                listAdapter = adapter
            }
        }
    }
}