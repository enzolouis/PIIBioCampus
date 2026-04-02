package com.fneb.piibiocampus.ui.admin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SearchUsersAdminActivity : BaseActivity() {

    private lateinit var adapter: UserAdapter
    private lateinit var recyclerView: RecyclerView

    private val allUsers = mutableListOf<UserProfile>()  // cache local complet
    private val displayedUsers = mutableListOf<UserProfile>()  // ce qu'on affiche

    private var currentQuery: String = ""

    private lateinit var searchEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searchusers_admin)
        setTopBarTitle(R.string.txtSearchAdmin)
        showTopBarLeftButton { finish() }

        recyclerView = findViewById(R.id.resultsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)

        adapter = UserAdapter(displayedUsers) { user, position ->
            AlertDialog.Builder(this)
                .setTitle("Bannir l'utilisateur")
                .setMessage("Voulez-vous vraiment bannir ${user.name} ?")
                .setPositiveButton("Bannir") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            UserDao.banUser(user.uid)
                            allUsers.remove(user)  // retire du cache aussi
                            animateBanAndRemove(position)
                            Toast.makeText(
                                this@SearchUsersAdminActivity,
                                "${user.name} a été banni.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@SearchUsersAdminActivity,
                                "Erreur : ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAllUsers()

        searchEditText.addTextChangedListener {
            val query = it.toString()
            if (query != currentQuery) {
                currentQuery = query
                applyFilter()
            }
        }
    }

    private fun loadAllUsers() {
        lifecycleScope.launch {
            try {
                val result = UserDao.getAllUsers()
                allUsers.clear()
                allUsers.addAll(result)
                applyFilter()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SearchUsersAdminActivity,
                    "Erreur de chargement : ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (currentQuery.isBlank()) {
            allUsers.toMutableList()
        } else {
            allUsers.filter {
                it.name?.lowercase()?.contains(currentQuery.lowercase()) == true
            }.toMutableList()
        }

        displayedUsers.clear()
        displayedUsers.addAll(filtered)
        adapter.notifyDataSetChanged()
    }

    private fun animateBanAndRemove(position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: run {
            if (position < displayedUsers.size) {
                displayedUsers.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
            return
        }

        val itemView = viewHolder.itemView

        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), Color.parseColor("#FFCDD2"), Color.TRANSPARENT).apply {
            duration = 700L
            addUpdateListener { itemView.setBackgroundColor(it.animatedValue as Int) }
        }

        val fadeOut = ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0f).apply {
            duration = 400L
            startDelay = 400L
        }

        AnimatorSet().apply {
            playTogether(colorAnim, fadeOut)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    itemView.alpha = 1f
                    if (position < displayedUsers.size) {
                        displayedUsers.removeAt(position)
                        adapter.notifyItemRemoved(position)
                    }
                }
            })
            start()
        }
    }
}