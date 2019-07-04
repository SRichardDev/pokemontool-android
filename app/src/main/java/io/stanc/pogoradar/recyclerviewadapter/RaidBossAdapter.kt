package io.stanc.pogoradar.recyclerviewadapter

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import io.stanc.pogoradar.FirebaseImageMapper
import io.stanc.pogoradar.FirebaseImageMapper.ASSETS_DIR_RAIDBOSSES
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.node.FirebaseRaidbossDefinition
import io.stanc.pogoradar.recyclerview.RecyclerViewAdapter


class RaidBossAdapter(private val context: Context,
                      private val raidBosses: List<FirebaseRaidbossDefinition>): RecyclerViewAdapter<FirebaseRaidbossDefinition>(context, raidBosses.toMutableList()) {

    override val itemLayoutRes: Int
        get() = R.layout.layout_list_item_raidboss

    override val clickableItemViewIdRes: Int
        get() = R.id.list_item_raidboss

    override val onlyOneItemIsSelectable: Boolean = true

    override fun onItemViewCreated(holder: Holder, id: Any) {

        raidBosses.find { it.id == id }?.let { raidBossDefinition ->

            val drawable = FirebaseImageMapper.assetDrawable(context, ASSETS_DIR_RAIDBOSSES, raidBossDefinition.imageName)
            holder.itemView.findViewById<ImageView>(R.id.list_item_raidboss_image).setImageDrawable(drawable)

            holder.itemView.findViewById<TextView>(R.id.list_item_raidboss_name).text = raidBossDefinition.name
        }
    }
}