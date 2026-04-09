package com.fneb.piibiocampus.ui.admin.settings.profile

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton

class EditProfileActivityAdmin : AppCompatActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

    private lateinit var editName:            EditText
    private lateinit var editOldPassword:     EditText
    private lateinit var editNewPassword:     EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var btnSave:             Button
    private lateinit var progressBar:         ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_edit_profile_admin)

        setTopBarTitle(R.string.titleEditProfile)
        showTopBarLeftButton { finish() }

        editName            = findViewById(R.id.editNameAdmin)
        editOldPassword     = findViewById(R.id.editOldPasswordAdmin)
        editNewPassword     = findViewById(R.id.editNewPasswordAdmin)
        editConfirmPassword = findViewById(R.id.editConfirmPasswordAdmin)
        btnSave             = findViewById(R.id.saveButtonAdmin)
        progressBar         = findViewById(R.id.progressBar)

        btnSave.setOnClickListener { handleSave() }

        observeViewModel()
        viewModel.loadProfile()
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleEditProfile)
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.profileState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    editName.setText(state.data)   // data = profile.name (String)
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                }
                else -> Unit
            }
        }

        viewModel.saveState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    showLoading(true)
                    btnSave.isEnabled = false
                }
                is UiState.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is UiState.Error -> {
                    showLoading(false)
                    btnSave.isEnabled = true
                    showError(state.exception)
                }
                else -> Unit
            }
        }
    }

    // ── Validation UI → délégation au ViewModel ───────────────────────────────

    private fun handleSave() {
        val name            = editName.text.toString().trim()
        val oldPassword     = editOldPassword.text.toString()
        val newPassword     = editNewPassword.text.toString()
        val confirmPassword = editConfirmPassword.text.toString()

        // Validation locale (règles UI, pas métier Firebase)
        if (name.isEmpty()) {
            editName.error = "Le pseudo ne peut pas être vide"
            return
        }

        val passwordChangeRequested =
            oldPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()

        if (passwordChangeRequested) {
            if (oldPassword.isEmpty()) {
                editOldPassword.error = "Entrez votre ancien mot de passe"
                return
            }
            if (newPassword.length < 6) {
                editNewPassword.error = "Le mot de passe doit faire au moins 6 caractères"
                return
            }
            if (newPassword != confirmPassword) {
                editConfirmPassword.error = "Les mots de passe ne correspondent pas"
                return
            }
        }

        viewModel.saveProfile(
            name        = name,
            oldPassword = if (passwordChangeRequested) oldPassword else null,
            newPassword = if (passwordChangeRequested) newPassword else null
        )
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}