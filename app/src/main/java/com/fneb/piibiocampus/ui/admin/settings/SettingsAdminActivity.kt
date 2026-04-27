package com.fneb.piibiocampus.ui.admin.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.admin.settings.cgu.CguActivityAdmin
import com.fneb.piibiocampus.ui.admin.settings.profile.EditProfileActivityAdmin
import com.fneb.piibiocampus.ui.auth.ConnectionActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsAdminActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var rowDeleteAccount: View
    private lateinit var progressBar:      ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        setTopBarTitle(R.string.titleSettings)
        showTopBarLeftButton { finish() }

        rowDeleteAccount = findViewById(R.id.rowDeleteAccountAdmin)
        progressBar      = findViewById(R.id.progressBar)

        setupClickListeners()
        observeViewModel()
        viewModel.loadUserRole()
    }

    // ── Listeners (stables) ───────────────────────────────────────────────────

    private fun setupClickListeners() {
        findViewById<View>(R.id.rowEditProfileAdmin).setOnClickListener {
            startActivity(Intent(this, EditProfileActivityAdmin::class.java))
        }

        findViewById<View>(R.id.rowSaveAdmin).setOnClickListener {
            viewModel.exportData(applicationContext)
        }

        findViewById<View>(R.id.rowDeconnexionAdmin).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Déconnexion")
                .setMessage("Es-tu sûr de vouloir te déconnecter ?")
                .setPositiveButton("Déconnexion") { _, _ -> viewModel.signOut(applicationContext) }
                .setNegativeButton("Annuler", null)
                .show()
        }

        findViewById<View>(R.id.rowCguConfidentialiteAdmin).setOnClickListener {
            startActivity(Intent(this, CguActivityAdmin::class.java))
        }

        rowDeleteAccount.setOnClickListener { showDeleteDialog() }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        // Rôle : masque la ligne supprimer si SUPER_ADMIN
        viewModel.isSuperAdmin.observe(this) { isSuperAdmin ->
            rowDeleteAccount.visibility = if (isSuperAdmin) View.GONE else View.VISIBLE
        }

        // Suppression du compte
        viewModel.deleteState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> { showLoading(false); navigateToLogin(); }   // navigation gérée par l'event
                is UiState.Error   -> { showLoading(false); showError(state.exception) }
                else -> Unit
            }
        }

        // Export CSV
        viewModel.exportState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    android.widget.Toast.makeText(this, "Export réussi", android.widget.Toast.LENGTH_SHORT).show()
                }
                is UiState.Error -> { showLoading(false); showError(state.exception) }
                else -> Unit
            }
        }

        // Événements ponctuels (navigation)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        SettingsEvent.NavigateToLogin -> navigateToLogin()
                    }
                }
            }
        }
    }

    // ── Dialogues ─────────────────────────────────────────────────────────────

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Supprimer mon compte")
            .setMessage("Tu es sur le point de supprimer définitivement ton compte…")
            .setPositiveButton("Supprimer") { _, _ ->
                // 2e dialog pour le mot de passe
                val input = android.widget.EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    hint = "Mot de passe"
                }
                android.app.AlertDialog.Builder(this)
                    .setTitle("Confirmer la suppression")
                    .setMessage("Saisis ton mot de passe pour confirmer.")
                    .setView(input)
                    .setPositiveButton("Confirmer") { _, _ ->
                        val password = input.text.toString()
                        if (password.isNotBlank()) viewModel.deleteAccount(password)
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun navigateToLogin() {
        startActivity(
            Intent(this, ConnectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}