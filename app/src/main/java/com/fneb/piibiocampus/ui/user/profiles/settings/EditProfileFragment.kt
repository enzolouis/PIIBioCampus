package com.fneb.piibiocampus.ui.user.profiles.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.Badge
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.PermissionFragment
import com.fneb.piibiocampus.utils.BadgeUtils
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class EditProfileFragment : PermissionFragment() {

    private lateinit var editProfilePicture:  ShapeableImageView
    private lateinit var editName:            EditText
    private lateinit var editDescription:     EditText
    private lateinit var currentBadgeImage:   ImageView
    private lateinit var currentBadgeLabel:   TextView
    private lateinit var editOldPassword:     EditText
    private lateinit var editNewPassword:     EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var saveButton:          Button
    private var progressBar: View? = null

    private val viewModel: EditProfileViewModel by viewModels { EditProfileViewModelFactory() }

    private var selectedBadgeId: String  = ""
    private var pendingImageBytes: ByteArray? = null

    // ── Galerie ───────────────────────────────────────────────────────────────

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val bytes = requireContext().contentResolver
            .openInputStream(uri)?.readBytes() ?: return@registerForActivityResult
        pendingImageBytes = bytes
        editProfilePicture.setImageURI(uri)
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editProfilePicture  = view.findViewById(R.id.editProfilePicture)
        editName            = view.findViewById(R.id.editName)
        editDescription     = view.findViewById(R.id.editDescription)
        currentBadgeImage   = view.findViewById(R.id.currentBadgeImage)
        currentBadgeLabel   = view.findViewById(R.id.currentBadgeLabel)
        editOldPassword     = view.findViewById(R.id.editOldPassword)
        editNewPassword     = view.findViewById(R.id.editNewPassword)
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword)
        saveButton          = view.findViewById(R.id.saveButton)
        progressBar         = view.findViewById(R.id.progressBar)

        view.findViewById<View>(R.id.editProfilePictureLabel).setOnClickListener { openGallery() }
        editProfilePicture.setOnClickListener { openGallery() }
        view.findViewById<View>(R.id.currentBadgeRow).setOnClickListener { showBadgePickerDialog() }
        saveButton.setOnClickListener { attemptSave() }

        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleEditProfile)
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    bindProfile(state.data)
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                }
                else -> Unit
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                }
                else -> Unit
            }
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindProfile(profile: UserProfile) {
        editName.setText(profile.name)
        editDescription.setText(profile.description)

        if (profile.profilePictureUrl.isNotEmpty()) {
            Picasso.get()
                .load(Uri.parse(profile.profilePictureUrl))
                .placeholder(R.drawable.photo_placeholder)
                .into(editProfilePicture)
        }

        selectedBadgeId = profile.currentBadge
        updateBadgeRow(selectedBadgeId)
    }

    // ── Galerie / permissions ─────────────────────────────────────────────────

    private fun openGallery() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        withPermission(permission = permission, onGranted = { pickImageLauncher.launch("image/*") })
    }

    // ── Badge ─────────────────────────────────────────────────────────────────

    private fun updateBadgeRow(badgeId: String) {
        val badge = BadgeUtils.ALL_BADGES.firstOrNull { it.id == badgeId }
        if (badge != null) {
            currentBadgeImage.setImageResource(badge.drawableRes)
            currentBadgeLabel.text = badge.label
        } else {
            currentBadgeImage.setImageResource(R.drawable.norank)
            currentBadgeLabel.text = "Aucun badge sélectionné"
        }
    }

    private fun showBadgePickerDialog() {
        val photoCount     = viewModel.photoCount.value ?: 0
        val unlockedBadges = BadgeUtils.getUnlockedBadges(photoCount)

        if (unlockedBadges.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun badge débloqué pour l'instant", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_badge_picker, null)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.badgeRecycler)
        recycler.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Choisir un badge")
            .setView(dialogView)
            .setNegativeButton("Annuler", null)
            .create()

        recycler.adapter = BadgePickerAdapter(unlockedBadges, selectedBadgeId) { badge ->
            selectedBadgeId = badge.id
            updateBadgeRow(badge.id)
            dialog.dismiss()
        }

        dialog.show()
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private fun attemptSave() {
        val name            = editName.text.toString().trim()
        val description     = editDescription.text.toString().trim()
        val oldPassword     = editOldPassword.text.toString()
        val newPassword     = editNewPassword.text.toString()
        val confirmPassword = editConfirmPassword.text.toString()

        // Validation locale — pas besoin du ViewModel pour ça
        if (name.isEmpty()) {
            editName.error = "Le pseudo ne peut pas être vide"
            return
        }

        val passwordChangeRequested = oldPassword.isNotEmpty()
                || newPassword.isNotEmpty()
                || confirmPassword.isNotEmpty()

        if (passwordChangeRequested) {
            if (oldPassword.isEmpty()) {
                editOldPassword.error = "Entrez votre ancien mot de passe"; return
            }
            if (newPassword.length < 6) {
                editNewPassword.error = "Le mot de passe doit faire au moins 6 caractères"; return
            }
            if (newPassword != confirmPassword) {
                editConfirmPassword.error = "Les mots de passe ne correspondent pas"; return
            }
        }

        viewModel.saveProfile(
            context         = requireContext(),
            name            = name,
            description     = description,
            selectedBadgeId = selectedBadgeId,
            imageBytes      = pendingImageBytes,
            oldPassword     = if (passwordChangeRequested) oldPassword else "",
            newPassword     = if (passwordChangeRequested) newPassword else ""
        )
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun showLoading(visible: Boolean) {
        progressBar?.visibility = if (visible) View.VISIBLE else View.GONE
        saveButton.isEnabled   = !visible
    }

    // ── Adapter badge picker ──────────────────────────────────────────────────

    inner class BadgePickerAdapter(
        private val badges:     List<Badge>,
        private val selectedId: String,
        private val onSelect:   (Badge) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<BadgePickerAdapter.BadgeViewHolder>() {

        inner class BadgeViewHolder(view: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.badgeItemImage)
            val label: TextView  = view.findViewById(R.id.badgeItemLabel)
            val check: View      = view.findViewById(R.id.badgeItemCheck)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_badge_picker, parent, false)
            return BadgeViewHolder(view)
        }

        override fun getItemCount() = badges.size

        override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
            val badge = badges[position]
            holder.image.setImageResource(badge.drawableRes)
            holder.label.text = badge.label
            holder.check.visibility = if (badge.id == selectedId) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener { onSelect(badge) }
        }
    }
}