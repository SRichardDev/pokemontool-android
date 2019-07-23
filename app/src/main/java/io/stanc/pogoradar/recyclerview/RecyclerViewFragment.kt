package io.stanc.pogoradar.recyclerview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


abstract class RecyclerViewFragment<ItemType: IdItem>: Fragment() {

    private val TAG = javaClass.name

    private var listAdapter: RecyclerViewAdapter<ItemType>? = null
    private var recyclerView: RecyclerView? = null
    private var dividerItemDecoration: DividerItemDecoration? = null

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

    open val showItemDivider: Boolean = true

    /**
     * interface
     */

    fun showList(itemList: List<ItemType>) {
        setupList(itemList)
    }

    fun selectedItem(): ItemType? {
        return listAdapter?.selectedItem
    }

    fun enableSelectItem(enabled: Boolean) {
        listAdapter?.selectionEnabled = enabled
    }

    fun deselectAllItems() {
        listAdapter?.deselectAllItems()
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

            context?.let { context ->
                val adapter = onCreateListAdapter(context, list)

                val layoutManager = LinearLayoutManager(context)
                layoutManager.orientation = orientation.value
                recyclerView.layoutManager = layoutManager

                if (showItemDivider) {
                    DividerItemDecoration(recyclerView.context, layoutManager.orientation).apply {

                        dividerItemDecoration?.let { recyclerView.removeItemDecoration(it) }
                        recyclerView.addItemDecoration(this)
                        dividerItemDecoration = this
                    }
                }

                recyclerView.adapter = adapter
                listAdapter?.selectionEnabled?.let { adapter.selectionEnabled = it }
                listAdapter = adapter
            }
        }
    }
}