package com.fneb.piibiocampus.ui.user.library

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso

class LibraryAdapter(
    private var items: List<SpeciesItem>,
    private val onClick: (SpeciesItem) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardRoot)
        val img:  ImageView        = view.findViewById(R.id.imgCensus)
        val name: TextView         = view.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species_library, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        // Toujours l'image de l'arbre taxonomique
        if (item.taxonomyImageUrl.isNotBlank()) {
            Picasso.get()
                .load(item.taxonomyImageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .fit().centerCrop()
                .into(holder.img)
        } else {
            holder.img.setImageResource(R.drawable.ic_placeholder_image)
        }

        // Grisage si l'espèce n'a pas encore été recensée par l'utilisateur·ice
        if (item.isRecordedByUser) {
            holder.img.colorFilter = null
            holder.img.alpha = 1f
        } else {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            holder.img.colorFilter = ColorMatrixColorFilter(matrix)
            holder.img.alpha = 0.55f
        }

        holder.card.setOnClickListener { onClick(item) }
    }

    fun update(newItems: List<SpeciesItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}