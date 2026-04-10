package com.fneb.piibiocampus.ui.admin.searchUsersAdmin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.UserProfile
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso

class UserAdapter(
    private val users: MutableList<UserProfile>,
    private val role: String?,
    private val onBanRequested: (UserProfile, Int) -> Unit,
    private val onRoleChangeRequested: (UserProfile, String, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val roles = listOf("USER", "ADMIN")

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.userAvatar)
        val pseudo: TextView = view.findViewById(R.id.userPseudo)
        val btnBan: MaterialButton = view.findViewById(R.id.btnBan)
        val spinnerRole: Spinner = view.findViewById(R.id.spinnerRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.pseudo.text = user.name

        // invisible :
        // holder.spinnerRole.visibility = if (role == "ADMIN") View.GONE else View.VISIBLE
        // grisé (mieux) :
        holder.spinnerRole.isEnabled = role == "SUPER_ADMIN"

        holder.btnBan.visibility = when {
            role == "SUPER_ADMIN" -> View.VISIBLE
            role == "ADMIN" && user.role == "USER" -> View.VISIBLE
            else -> View.GONE
        }

        Picasso.get().cancelRequest(holder.avatar)
        if (!user.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get().load(user.profilePictureUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .fit().centerCrop().into(holder.avatar)
        } else {
            holder.avatar.setImageResource(R.drawable.photo_placeholder)
        }

        val adapterSpinner = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, roles)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinnerRole.adapter = adapterSpinner

        holder.spinnerRole.onItemSelectedListener = null
        val currentIndex = roles.indexOf(user.role)
        if (currentIndex >= 0) holder.spinnerRole.setSelection(currentIndex, false)

        holder.spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val newRole = roles[pos]
                if (newRole != user.role) {
                    onRoleChangeRequested(user, newRole, holder.adapterPosition)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        holder.btnBan.setOnClickListener { onBanRequested(user, holder.adapterPosition) }
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<UserProfile>) {
        users.clear()
        users.addAll(newList)
        notifyDataSetChanged()
    }
}