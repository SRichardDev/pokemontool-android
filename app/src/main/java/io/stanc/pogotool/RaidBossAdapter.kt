package io.stanc.pogotool

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.firebase.data.FirebaseRaidboss
import android.databinding.adapters.ImageViewBindingAdapter.setImageDrawable
import android.graphics.drawable.Drawable
import android.view.View
import java.io.IOException


class RaidBossAdapter(private val context: Context,
                      private val raidBosses: List<FirebaseRaidboss>,
                      private val onItemClickListener: OnItemClickListener): RecyclerView.Adapter<RaidBossAdapter.RaidBossHolder>() {

    private val TAG = javaClass.name
    private val itemViews = mutableListOf<View>()
    private var selectedItem: FirebaseRaidboss? = null

    interface OnItemClickListener {
        fun onClick(id: String)
    }

    fun getItem(id: String): FirebaseRaidboss {
        return raidBosses.first { it.id == id }
    }

    fun getSelectedItem(): FirebaseRaidboss? = selectedItem

    inner class RaidBossHolder(var id: String?, itemLayout: View) : RecyclerView.ViewHolder(itemLayout) {
        init {
            itemViews.add(itemLayout)
            val imageView = itemLayout.findViewById<ImageView>(R.id.list_item_raidboss_image)
            imageView.setOnClickListener {
                id?.let {
                    onItemClickListener.onClick(it)
                    deselectAllItems()
                    itemLayout.isSelected = true
                    selectedItem = getItem(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaidBossAdapter.RaidBossHolder {
        val itemLayout = LayoutInflater.from(context).inflate(R.layout.layout_list_item_raidboss, parent, false)
        return RaidBossHolder(null, itemLayout)
    }

    override fun onBindViewHolder(holder: RaidBossHolder, position: Int) {
        holder.itemView.isSelected = false

        holder.id = raidBosses[position].id

        try {
            val inputStream = context.assets.open("${raidBosses[position].imageName}.png")
            val drawable = Drawable.createFromStream(inputStream, null)
            holder.itemView.findViewById<ImageView>(R.id.list_item_raidboss_image).setImageDrawable(drawable)

        } catch (ex: IOException) {
            Log.e(TAG, ex.toString())
        }

        holder.itemView.findViewById<TextView>(R.id.list_item_raidboss_name).text = raidBosses[position].name
    }

    override fun getItemCount() = raidBosses.size

    private fun deselectAllItems() {
        itemViews.forEach { it.isSelected = false }
    }
}