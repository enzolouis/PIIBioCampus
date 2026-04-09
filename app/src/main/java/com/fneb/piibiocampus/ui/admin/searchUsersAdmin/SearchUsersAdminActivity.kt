package com.fneb.piibiocampus.ui.admin.searchUsersAdmin

import android.animation.*
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.google.android.material.textfield.TextInputEditText

class SearchUsersAdminActivity : BaseActivity() {

    private lateinit var viewModel: SearchUsersAdminViewModel
    private lateinit var adapter: UserAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private val displayedUsers = mutableListOf<UserProfile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searchusers_admin)

        // Initialisation du ViewModel (Old School)
        viewModel = ViewModelProvider(this).get(SearchUsersAdminViewModel::class.java)

        setTopBarTitle(R.string.txtSearchAdmin)
        showTopBarLeftButton { finish() }

        recyclerView = findViewById(R.id.resultsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        val role = intent.getStringExtra("role")

        setupRecyclerView(role)
        setupListeners()
        observeViewModel()

        viewModel.loadAllUsers()
    }

    private fun setupRecyclerView(role: String?) {
        adapter = UserAdapter(
            users = displayedUsers,
            role = role,
            onBanRequested = { user, pos -> showBanDialog(user, pos) },
            onRoleChangeRequested = { user, newRole, pos -> showRoleDialog(user, newRole, pos) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        searchEditText.addTextChangedListener {
            viewModel.applyFilter(it.toString())
        }
    }

    private fun observeViewModel() {
        // Observation de la liste des utilisateurs
        viewModel.usersState.observe(this) { state ->
            when (state) {
                is UiState.Success -> adapter.updateList(state.data)
                is UiState.Error -> showError(state.exception)
                else -> Unit
            }
        }

        // Observation des actions (Ban / Rôle)
        viewModel.actionState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    val (position, message) = state.data
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (message.contains("banni")) {
                        animateBanAndRemove(position)
                    }
                }
                is UiState.Error -> {
                    showError(state.exception)
                    adapter.notifyDataSetChanged() // Remet le spinner à l'état précédent en cas d'erreur
                }
                else -> Unit
            }
        }
    }

    private fun showBanDialog(user: UserProfile, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Bannir l'utilisateur")
            .setMessage("Voulez-vous vraiment bannir ${user.name} ?")
            .setPositiveButton("Bannir") { _, _ -> viewModel.banUser(user, position) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showRoleDialog(user: UserProfile, newRole: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Changer le rôle")
            .setMessage("Passer ${user.name} en $newRole ?")
            .setPositiveButton("Confirmer") { _, _ -> viewModel.updateUserRole(user, newRole, position) }
            .setNegativeButton("Annuler") { _, _ -> adapter.notifyItemChanged(position) }
            .show()
    }

    // Ta logique d'animation originale conservée telle quelle
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