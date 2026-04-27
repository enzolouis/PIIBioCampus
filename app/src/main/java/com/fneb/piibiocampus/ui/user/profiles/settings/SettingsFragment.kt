package com.fneb.piibiocampus.ui.user.profiles.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.PermissionFragment
import com.fneb.piibiocampus.ui.auth.ConnectionActivity
import com.fneb.piibiocampus.utils.setTopBarTitle

class SettingsFragment : PermissionFragment() {

    private var progressBar: View? = null

    private val viewModel: SettingsViewModel by viewModels { SettingsViewModelFactory() }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)

        setupButtons(view)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleSettings)
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    private fun setupButtons(view: View) {
        view.findViewById<View>(R.id.rowEditProfile).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowSave).setOnClickListener {
            withPermission(
                permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                onGranted  = { viewModel.exportData(requireContext()) }
            )
        }

        view.findViewById<View>(R.id.rowDeconnexion).setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Déconnexion")
                .setMessage("Es-tu sûr de vouloir te déconnecter ?")
                .setPositiveButton("Déconnexion") { _, _ ->
                    viewModel.signOut(requireContext().applicationContext)
                    navigateToAuth()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        view.findViewById<View>(R.id.rowCguConfidentialite).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CguFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowDeleteAccount).setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Supprimer mon compte")
                .setMessage(
                    "Tu es sur le point de supprimer définitivement ton compte ainsi que toutes " +
                            "tes données associées (photos, observations…). Cette action est irréversible."
                )
                .setPositiveButton("Supprimer") { _, _ ->
                    // 2e dialog pour le mot de passe
                    val input = android.widget.EditText(requireContext()).apply {
                        inputType = android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        hint = "Mot de passe"
                    }
                    android.app.AlertDialog.Builder(requireContext())
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
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.exportState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Export enregistré : ${state.data}",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetExportState()
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                    viewModel.resetExportState()
                }
                else -> Unit
            }
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    viewModel.resetDeleteState()
                    navigateToAuth()
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                    viewModel.resetDeleteState()
                }
                else -> Unit
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun showLoading(visible: Boolean) {
        progressBar?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun navigateToAuth() {
        startActivity(
            Intent(requireContext(), ConnectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}