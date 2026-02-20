package com.example.piibiocampus.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.model.UserProfile
import com.google.android.material.button.MaterialButton

class UserAdapter(
    private val users: MutableList<UserProfile>,
    private val onBan: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.userAvatar)
        val pseudo: TextView = view.findViewById(R.id.userPseudo)
        val btnBan: MaterialButton = view.findViewById(R.id.btnBan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.pseudo.text = user.name

        holder.btnBan.setOnClickListener {
            onBan(user)
        }
    }

    override fun getItemCount() = users.size

    fun addMore(newUsers: List<UserProfile>) {
        val start = users.size
        users.addAll(newUsers)
        notifyItemRangeInserted(start, newUsers.size)
    }
}
