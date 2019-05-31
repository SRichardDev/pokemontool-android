package io.stanc.pogotool.recyclerviewadapter

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogotool.FirebaseImageMapper
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogotool.recyclerview.RecyclerViewAdapter


class RaidBossAdapter(private val context: Context,
                      private val raidBosses: List<FirebaseRaidbossDefinition>): RecyclerViewAdapter<FirebaseRaidbossDefinition>(context, raidBosses.toMutableList()) {

    override val itemLayoutRes: Int
        get() = R.layout.layout_list_item_raidboss

    override val clickableItemViewIdRes: Int
        get() = R.id.list_item_raidboss

    override val onlyOneItemIsSelectable: Boolean = true

    override fun onItemViewCreated(holder: Holder, id: Any) {

        raidBosses.find { it.id == id }?.let { raidBossDefinition ->

            val drawable = FirebaseImageMapper.raidBossDrawable(context, raidBossDefinition.imageName)
            holder.itemView.findViewById<ImageView>(R.id.list_item_raidboss_image).setImageDrawable(drawable)

            holder.itemView.findViewById<TextView>(R.id.list_item_raidboss_name).text = raidBossDefinition.name
        }
    }
}