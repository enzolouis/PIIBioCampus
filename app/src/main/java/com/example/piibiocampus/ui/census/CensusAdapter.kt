package com.example.piibiocampus.ui.census

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.census.CensusNode
import com.squareup.picasso.Picasso

class CensusAdapter(
    private val items: List<CensusNode>,
    private val onItemClick: (CensusNode, Int) -> Unit,
    private val onInfoClick: (CensusNode) -> Unit
) : RecyclerView.Adapter<CensusAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgCensus)
        val name: TextView = view.findViewById(R.id.tvName)
        val btnInfo: ImageView = view.findViewById(R.id.btnInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_census_sqaure, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        if (item.imageUrl.isNotBlank()) {
            Picasso.get()
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .fit()
                .centerCrop()
                .into(holder.img)
        } else {
            holder.img.setImageResource(R.drawable.ic_placeholder_image)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item, position)
        }
        holder.btnInfo.setOnClickListener {
            onInfoClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
