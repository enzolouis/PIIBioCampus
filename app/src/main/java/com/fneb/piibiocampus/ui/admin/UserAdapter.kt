package com.fneb.piibiocampus.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.model.UserProfile
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import kotlin.sequences.forEach

class UserAdapter(
    private val users: MutableList<UserProfile>,
    private val role: String?,
    private val onBan: (UserProfile, Int) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val roles = listOf("USER", "ADMIN")

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.userAvatar)
        val pseudo: TextView = view.findViewById(R.id.userPseudo)
        val btnBan: MaterialButton = view.findViewById(R.id.btnBan)
        val spinnerRole: Spinner = view.findViewById(R.id.spinnerRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.pseudo.text = user.name

        if (role == "ADMIN"){
           holder.spinnerRole.visibility = View.GONE
        }else{
            holder.spinnerRole.visibility = View.VISIBLE
        }

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

        // Spinner setup
        val adapterSpinner = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            roles
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        holder.spinnerRole.adapter = adapterSpinner

        holder.spinnerRole.onItemSelectedListener = null

        // sélection actuelle
        val currentIndex = roles.indexOf(user.role)
        if (currentIndex >= 0) {
            holder.spinnerRole.setSelection(currentIndex, false)
        }

        // Listener
        holder.spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val newRole = roles[pos]

                // éviter appel inutile
                if (newRole != user.role) {

                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Changer le rôle")
                        .setMessage("Passer ${user.name} en $newRole ?")
                        .setPositiveButton("Confirmer") { _, _ ->

                            UserDao.updateUserRole(
                                userId = user.uid,
                                newRole = newRole,
                                onSuccess = {
                                    user.role = newRole
                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Rôle mis à jour",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { e ->
                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Erreur : ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        .setNegativeButton("Annuler") { _, _ ->
                            // remettre l'ancien rôle visuellement
                            val oldIndex = roles.indexOf(user.role)
                            holder.spinnerRole.setSelection(oldIndex, false)
                        }
                        .show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        holder.btnBan.setOnClickListener {
            onBan(user, holder.adapterPosition)
        }
    }

    override fun getItemCount() = users.size
}