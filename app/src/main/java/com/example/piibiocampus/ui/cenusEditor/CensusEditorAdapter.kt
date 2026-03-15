package com.example.piibiocampus.ui.census

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Picasso

class CensusEditorAdapter(
    private var items: List<CensusNode>,
    private val onNavigate: (CensusNode) -> Unit,
    private val onEdit:     (CensusNode) -> Unit,
    private val onDelete:   (CensusNode) -> Unit,
    private val onAdd:      () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NODE = 0
        private const val TYPE_ADD  = 1
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    inner class NodeVH(view: View) : RecyclerView.ViewHolder(view) {
        val card:      MaterialCardView = view.findViewById(R.id.cardRoot)
        val img:       ImageView        = view.findViewById(R.id.imgCensus)
        val name:      TextView         = view.findViewById(R.id.tvName)
        val btnEdit:   MaterialButton   = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton   = view.findViewById(R.id.btnDeleteNode)
    }

    inner class AddVH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardAdd)
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) =
        if (position < items.size) TYPE_NODE else TYPE_ADD

    override fun getItemCount() = items.size + 1  // +1 pour la carte "+"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_NODE)
            NodeVH(inf.inflate(R.layout.item_census_editor, parent, false))
        else
            AddVH(inf.inflate(R.layout.item_census_add, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NodeVH -> bindNode(holder, items[position])
            is AddVH  -> holder.card.setOnClickListener { onAdd() }
        }
    }

    private fun bindNode(holder: NodeVH, item: CensusNode) {
        holder.name.text = item.name

        if (item.imageUrl.isNotBlank()) {
            Picasso.get()
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .fit().centerCrop()
                .into(holder.img)
        } else {
            holder.img.setImageResource(R.drawable.ic_placeholder_image)
        }

        holder.card.setOnClickListener {
            if (item.type != CensusType.SPECIES) onNavigate(item)
        }

        holder.btnEdit.setOnClickListener   { onEdit(item)   }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    // ── Mise à jour ───────────────────────────────────────────────────────────

    fun update(newItems: List<CensusNode>) {
        items = newItems
        notifyDataSetChanged()
    }
}