package io.stanc.pogotool.recyclerview

import android.content.Context
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


abstract class RecyclerViewAdapter<ItemType: IdItem>(private val context: Context,
                                   private var itemList: MutableList<ItemType>): RecyclerView.Adapter<RecyclerViewAdapter<ItemType>.Holder>() {
    private val TAG = javaClass.name

    private val itemViews = mutableListOf<View>()
    var selectedItem: ItemType? = null
        private set

    /**
     * customize
     */

    @get:LayoutRes
    abstract val itemLayoutRes: Int

    @get:IdRes
    abstract val clickableItemViewIdRes: Int

    abstract fun onItemViewCreated(holder: Holder, id: Any)

    interface OnItemClickListener {
        fun onClick(id: Any)
    }

    abstract val onlyOneItemIsSelectable: Boolean

    /**
     * interface
     */

    var onItemClickListener: OnItemClickListener? = null

    fun getItem(id: Any): ItemType = itemList.first { it.id == id }

    override fun getItemCount() = itemList.size

    fun deselectAllItems() {
        itemViews.forEach {
            it.isSelected = false
        }
        selectedItem = null
    }

    fun updateList(newItemList: List<ItemType>) {
        itemViews.clear()
        itemList.clear()
        itemList.addAll(newItemList)
        notifyDataSetChanged()
    }

    /**
     * private
     */

    inner class Holder(var id: Any?, itemLayout: View) : RecyclerView.ViewHolder(itemLayout) {
        init {
            itemViews.add(itemLayout)
            itemLayout.findViewById<View>(clickableItemViewIdRes).setOnClickListener {
                id?.let {
                    selectItem(itemLayout, it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemLayout = LayoutInflater.from(context).inflate(itemLayoutRes, parent, false)
        return Holder(null, itemLayout)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.itemView.isSelected = false

        itemList[position].id.apply {
            holder.id = this
            onItemViewCreated(holder, this)
        }
    }

    private fun selectItem(itemLayout: View, itemId: Any) {

        if (onlyOneItemIsSelectable) {
            deselectAllItems()
        }

        itemLayout.isSelected = true
        selectedItem = getItem(itemId)

        onItemClickListener?.onClick(itemId)
    }
}