package com.example.piibiocampus.ui.searchUsers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.model.UserProfile
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class SearchUserAdapter(
    private val items: List<UserProfile>,
    private val onUserClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.userPseudo)
        val avatar: ShapeableImageView = view.findViewById(R.id.userAvatar)
        val desc: TextView = view.findViewById(R.id.userDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = items[position]
        holder.name.text = user.name
        holder.desc.text = user.description

        if (!user.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.photo_placeholder)
                .into(holder.avatar)
        } else {
            holder.avatar.setImageResource(R.drawable.photo_placeholder)
        }

        holder.itemView.setOnClickListener { onUserClick(user) }
    }
}