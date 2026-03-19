package com.fneb.piibiocampus.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.UserProfile
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso

class UserAdapter(
    private val users: MutableList<UserProfile>,
    private val onBan: (UserProfile, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.userAvatar)
        val pseudo: TextView = view.findViewById(R.id.userPseudo)
        val btnBan: MaterialButton = view.findViewById(R.id.btnBan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.pseudo.text = user.name

        Picasso.get().cancelRequest(holder.avatar)

        if (!user.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(user.profilePictureUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .fit()
                .centerCrop()
                .into(holder.avatar)
        } else {
            holder.avatar.setImageResource(R.drawable.photo_placeholder)
        }
        holder.btnBan.setOnClickListener {
            onBan(user, holder.adapterPosition)
        }
    }

    override fun getItemCount() = users.size
}