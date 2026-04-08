package com.fneb.piibiocampus.ui.photo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.ui.census.CensusMode
import com.fneb.piibiocampus.ui.census.CensusTreeActivity
import com.fneb.piibiocampus.ui.profiles.UserProfileFragment
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class PicturesViewerFragment : DialogFragment() {

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private val viewModel: PicturesViewerViewModel by viewModels {
        val state = arguments?.getParcelable<PhotoViewerState>(ARG_STATE)
            ?: error("PicturesViewerFragment: état manquant dans les arguments")
        PicturesViewerViewModel.factory(state)
    }

    // ── Vues ──────────────────────────────────────────────────────────────────

    private var ivAuthorProfile: ImageView? = null
    private var tvTitle: TextView?          = null
    private var tvValidationStatus: TextView? = null
    private var photoView: PhotoView?       = null
    private var tvDate: TextView?           = null
    private var tvDescription: TextView?   = null
    private var btnResumeCensus: Button?   = null
    private var btnValidate: Button?       = null
    private var btnBack: Button?           = null
    private var btnDelete: Button?         = null

    // ── Launcher recensement ──────────────────────────────────────────────────

    private val censusLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.reloadPicture()
            }
        }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG       = "PicturesViewerFragment"
        private const val ARG_STATE = "state"
        const val RESULT_DELETED    = "picture_deleted"
        const val REQUEST_KEY       = "PicturesViewerFragment"

        fun show(fm: FragmentManager, state: PhotoViewerState) {
            PicturesViewerFragment().apply {
                arguments = Bundle().apply { putParcelable(ARG_STATE, state) }
            }.show(fm, TAG)
        }
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogPopup)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pictures_viewer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupClickListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dm = resources.displayMetrics
            window.setLayout((dm.widthPixels * 0.92f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Éviter les fuites mémoire sur les références de vues
        ivAuthorProfile = null
        tvTitle         = null
        tvValidationStatus = null
        photoView       = null
        tvDate          = null
        tvDescription   = null
        btnResumeCensus = null
        btnValidate     = null
        btnBack         = null
        btnDelete       = null
    }

    // ── Initialisation des vues ───────────────────────────────────────────────

    private fun bindViews(view: View) {
        ivAuthorProfile    = view.findViewById(R.id.ivAuthorProfile)
        tvTitle            = view.findViewById(R.id.tvTitle)
        tvValidationStatus = view.findViewById(R.id.tvValidationStatus)
        photoView          = view.findViewById(R.id.photoView)
        tvDate             = view.findViewById(R.id.tvDate)
        tvDescription      = view.findViewById(R.id.tvDescription)
        btnResumeCensus    = view.findViewById(R.id.btnResumeCensus)
        btnValidate        = view.findViewById(R.id.btnValidate)
        btnBack            = view.findViewById(R.id.btnBack)
        btnDelete          = view.findViewById(R.id.btnDelete)
    }

    // ── Listeners (stables, indépendants de l'état) ───────────────────────────

    private fun setupClickListeners() {
        btnBack?.setOnClickListener { dismiss() }

        ivAuthorProfile?.setOnClickListener {
            val userRef = viewModel.uiState.value.userRef
            dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserProfileFragment.newInstance(userRef))
                .addToBackStack(null)
                .commit()
        }

        btnValidate?.setOnClickListener {
            val isValidated = viewModel.uiState.value.adminValidated
            val message = if (!isValidated) "Valider cette photo ?" else "Invalider cette photo ?"
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmation")
                .setMessage(message)
                .setPositiveButton("Oui") { _, _ -> viewModel.validatePicture() }
                .setNegativeButton("Annuler", null)
                .show()
        }

        btnDelete?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer la photo")
                .setMessage("Cette action est irréversible. Confirmer la suppression ?")
                .setPositiveButton("Supprimer") { _, _ -> viewModel.deletePicture() }
                .setNegativeButton("Annuler", null)
                .show()
        }

        btnResumeCensus?.setOnClickListener {
            val state = viewModel.uiState.value
            val intent = Intent(requireContext(), CensusTreeActivity::class.java).apply {
                putExtra("mode",      CensusMode.UPDATE.name)
                putExtra("pictureId", state.pictureId)
                putExtra("imageUrl",  state.imageUrl)
                putExtra("caller",    state.caller.name)
                state.censusRef?.takeIf { it.isNotEmpty() && it != "null" }
                    ?.let { putExtra("initialNodeId", it) }
            }
            censusLauncher.launch(intent)
        }
    }

    // ── Observation du ViewModel ──────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // État UI
                launch {
                    viewModel.uiState.collect { state -> renderState(state) }
                }

                // Événements ponctuels
                launch {
                    viewModel.events.collect { event -> handleEvent(event) }
                }
            }
        }
    }

    // ── Rendu complet depuis l'état ───────────────────────────────────────────

    private fun renderState(state: PhotoViewerState) {
        // Photo principale (chargée une seule fois — l'URL ne change pas)
        photoView?.let { pv ->
            Picasso.get().load(state.imageUrl)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .into(pv)
        }

        // Photo de profil de l'auteur
        if (state.showAuthorProfile && !state.profilePictureUrl.isNullOrEmpty()) {
            ivAuthorProfile?.isVisible = true
            ivAuthorProfile?.let {
                Picasso.get().load(state.profilePictureUrl)
                    .placeholder(R.drawable.user_circle)
                    .transform(CircleTransform())
                    .into(it)
            }
        } else {
            ivAuthorProfile?.isVisible = false
        }

        // Titre et métadonnées taxonomiques
        tvTitle?.text = state.displayTitle
        tvDate?.text  = state.timestamp
        tvDescription?.text = buildString {
            appendLine("Famille : ${state.family ?: "Non identifiée"}")
            appendLine("Genre   : ${state.genre   ?: "Non identifié"}")
            append(    "Espèce  : ${state.specie  ?: "Non identifiée"}")
        }

        // Statut de validation
        renderValidationStatus(state)

        // Bouton "Valider / Invalider" (admin)
        btnValidate?.isVisible = state.showValidate
        if (state.showValidate) renderValidateButton(state)

        // Bouton "Reprendre le recensement"
        btnResumeCensus?.isVisible = state.showResumeCensus

        // Bouton "Supprimer"
        btnDelete?.isVisible = state.showDelete
    }

    // ── Gestion des événements ────────────────────────────────────────────────

    private fun handleEvent(event: PicturesViewerEvent) {
        when (event) {
            is PicturesViewerEvent.ShowToast -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            is PicturesViewerEvent.PictureValidated -> {
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {
                        putString("validated_picture_id", event.pictureId)
                        putBoolean("validated_value", event.newValue)
                    }
                )
            }
            is PicturesViewerEvent.PictureDeleted -> {
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {
                        putBoolean(RESULT_DELETED, true)
                        putString("deleted_picture_id", event.pictureId)
                    }
                )
            }
            is PicturesViewerEvent.CensusUpdated -> {
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply { putBoolean("census_updated", true) }
                )
            }
            PicturesViewerEvent.Dismiss -> dismiss()
        }
    }

    // ── Helpers de rendu ─────────────────────────────────────────────────────

    private fun renderValidationStatus(state: PhotoViewerState) {
        tvValidationStatus?.apply {
            isVisible = true
            when {
                state.adminValidated -> {
                    text = "✓ Validé par un administrateur"
                    setTextColor(Color.parseColor("#4CAF50"))
                }
                !state.recordingStatus -> {
                    text = "● Recensement non terminé"
                    setTextColor(Color.parseColor("#F44336"))
                }
                else -> {
                    text = "○ Recensement non validé par un admin"
                    setTextColor(Color.parseColor("#FF9800"))
                }
            }
        }
    }

    private fun renderValidateButton(state: PhotoViewerState) {
        btnValidate?.apply {
            if (state.adminValidated) {
                text = "Invalider"
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
            } else {
                text = "Valider"
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
            }
        }
    }
}