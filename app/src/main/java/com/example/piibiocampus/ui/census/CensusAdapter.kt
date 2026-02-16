package com.example.piibiocampus.ui.census

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.census.CensusNode
import com.squareup.picasso.Picasso
import com.google.android.material.card.MaterialCardView

class CensusAdapter(
    private var items: List<CensusNode>,
    private var selectedNodeId: String?,
    private val onItemClick: (CensusNode, Int) -> Unit,
    private val onInfoClick: (CensusNode) -> Unit
) : RecyclerView.Adapter<CensusAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgCensus)
        val name: TextView = view.findViewById(R.id.tvName)
        val btnInfo: ImageView = view.findViewById(R.id.btnInfo)
        val card: MaterialCardView = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_census_sqaure, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        if (item.imageUrl.isNotBlank()) {
            Picasso.get().load(item.imageUrl).placeholder(R.drawable.ic_placeholder_image).fit().centerCrop().into(holder.img)
        } else {
            holder.img.setImageResource(R.drawable.ic_placeholder_image)
        }

        // highlight si selectionné
        if (item.id == selectedNodeId) {
            holder.card.strokeWidth = (3 * holder.card.context.resources.displayMetrics.density).toInt()
            holder.card.strokeColor = ContextCompat.getColor(holder.card.context, R.color.primary)
        } else {
            holder.card.strokeWidth = 0
        }

        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }

        holder.btnInfo.setOnClickListener {
            onInfoClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(itemsNew: List<CensusNode>, selectedId: String?) {
        this.items = itemsNew
        this.selectedNodeId = selectedId
        notifyDataSetChanged()
    }
}