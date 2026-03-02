package com.example.piibiocampus.ui.photo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.PictureDao
import com.example.piibiocampus.ui.census.CensusMode
import com.example.piibiocampus.ui.census.CensusTreeActivity
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class PicturesViewerFragment : DialogFragment() {

    private lateinit var state: PhotoViewerState
    private var btnValidateRef: Button?    = null
    private var tvStatusRef: TextView?     = null
    private var btnResumeCensusRef: Button? = null

    // Références aux vues pour mise à jour après reload
    private var tvTitleRef: TextView?       = null
    private var tvDescriptionRef: TextView? = null

    // ── Launcher pour recensement ─────────────────────────────────────────────
    private val censusLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Recharger la photo enrichie depuis Firestore pour avoir
                // les données taxonomiques à jour (famille/genre/espèce)
                reloadPictureAndRefreshUI()
            }
        }

    companion object {
        private const val TAG        = "PicturesViewerFragment"
        private const val ARG_STATE  = "state"
        const val RESULT_DELETED     = "picture_deleted"
        const val REQUEST_KEY        = "PicturesViewerFragment"

        fun show(fm: FragmentManager, state: PhotoViewerState) {
            PicturesViewerFragment().apply {
                arguments = Bundle().apply { putParcelable(ARG_STATE, state) }
            }.show(fm, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = arguments?.getParcelable(ARG_STATE) ?: run { dismiss(); return }
        setStyle(STYLE_NO_TITLE, R.style.DialogPopup)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pictures_viewer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivAuthorProfile    = view.findViewById<ImageView>(R.id.ivAuthorProfile)
        val tvTitle            = view.findViewById<TextView>(R.id.tvTitle)
        val tvValidationStatus = view.findViewById<TextView>(R.id.tvValidationStatus)
        val photoView          = view.findViewById<PhotoView>(R.id.photoView)
        val tvDate             = view.findViewById<TextView>(R.id.tvDate)
        val tvDescription      = view.findViewById<TextView>(R.id.tvDescription)
        val btnResumeCensus    = view.findViewById<Button>(R.id.btnResumeCensus)
        val btnValidate        = view.findViewById<Button>(R.id.btnValidate)
        val btnBack            = view.findViewById<Button>(R.id.btnBack)
        val btnDelete          = view.findViewById<Button>(R.id.btnDelete)

        btnValidateRef    = btnValidate
        tvStatusRef       = tvValidationStatus
        btnResumeCensusRef = btnResumeCensus
        tvTitleRef        = tvTitle
        tvDescriptionRef  = tvDescription

        bindState(
            ivAuthorProfile, tvTitle, tvValidationStatus,
            photoView, tvDate, tvDescription,
            btnResumeCensus, btnValidate, btnBack, btnDelete
        )
    }

    // ── Binding complet de l'état dans les vues ───────────────────────────────

    private fun bindState(
        ivAuthorProfile: ImageView,
        tvTitle: TextView,
        tvValidationStatus: TextView,
        photoView: PhotoView,
        tvDate: TextView,
        tvDescription: TextView,
        btnResumeCensus: Button,
        btnValidate: Button,
        btnBack: Button,
        btnDelete: Button
    ) {
        // Photo principale
        Picasso.get().load(state.imageUrl)
            .placeholder(R.drawable.photo_placeholder)
            .error(R.drawable.photo_placeholder)
            .into(photoView)

        // Photo de profil auteur
        if (state.showAuthorProfile && !state.profilePictureUrl.isNullOrEmpty()) {
            ivAuthorProfile.visibility = View.VISIBLE
            Picasso.get().load(state.profilePictureUrl)
                .placeholder(R.drawable.user_circle)
                .transform(CircleTransform())
                .into(ivAuthorProfile)
        } else ivAuthorProfile.visibility = View.GONE

        // Titre et description
        tvTitle.text = state.displayTitle
        tvDate.text  = state.timestamp
        tvDescription.text = buildString {
            appendLine("Famille : ${state.family ?: "Non identifiée"}")
            appendLine("Genre   : ${state.genre   ?: "Non identifié"}")
            append(    "Espèce  : ${state.specie  ?: "Non identifiée"}")
        }

        // Statut
        refreshValidationStatus(tvValidationStatus)

        // Reprendre le recensement
        if (state.showResumeCensus) {
            btnResumeCensus.visibility = View.VISIBLE
            btnResumeCensus.text = "Reprendre le recensement"
            btnResumeCensus.setOnClickListener {
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
        } else btnResumeCensus.visibility = View.GONE

        // Valider / invalider (admin)
        if (state.showValidate) {
            btnValidate.visibility = View.VISIBLE
            refreshValidateButton()
            btnValidate.setOnClickListener {
                val newValue = !state.adminValidated
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirmation")
                    .setMessage(if (newValue) "Valider cette photo ?" else "Invalider cette photo ?")
                    .setPositiveButton("Oui") { _, _ ->
                        PictureDao.setAdminValidated(state.pictureId, newValue,
                            onSuccess = {
                                state = state.copy(adminValidated = newValue)
                                refreshValidateButton()
                                refreshValidationStatus(tvStatusRef)
                                btnResumeCensus.visibility =
                                    if (state.showResumeCensus) View.VISIBLE else View.GONE
                                notifyParent()
                                Toast.makeText(requireContext(),
                                    if (newValue) "Photo validée" else "Photo invalidée",
                                    Toast.LENGTH_SHORT).show()
                            },
                            onError = { e ->
                                Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .setNegativeButton("Annuler", null).show()
            }
        } else btnValidate.visibility = View.GONE

        // Retour
        btnBack.setOnClickListener { dismiss() }

        // Supprimer
        if (state.showDelete) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer la photo")
                    .setMessage("Cette action est irréversible. Confirmer la suppression ?")
                    .setPositiveButton("Supprimer") { _, _ ->
                        PictureDao.deletePicture(state.pictureId,
                            onSuccess = {
                                Toast.makeText(requireContext(), "Photo supprimée", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.setFragmentResult(
                                    REQUEST_KEY,
                                    Bundle().apply { putBoolean(RESULT_DELETED, true) }
                                )
                                dismiss()
                            },
                            onError = { e ->
                                Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .setNegativeButton("Annuler", null).show()
            }
        } else btnDelete.visibility = View.GONE
    }

    // ── Rechargement depuis Firestore après retour du recensement ─────────────

    private fun reloadPictureAndRefreshUI() {
        PictureDao.getPictureEnrichedById(
            pictureId = state.pictureId,
            onSuccess = { updatedPhoto ->
                // Reconstruire le state avec les nouvelles données taxonomiques
                state = state.copy(
                    family          = updatedPhoto["family"]          as? String,
                    genre           = updatedPhoto["genre"]           as? String,
                    specie          = updatedPhoto["specie"]          as? String,
                    censusRef       = updatedPhoto["censusRef"]       as? String,
                    recordingStatus = updatedPhoto["recordingStatus"] as? Boolean ?: false,
                    adminValidated  = updatedPhoto["adminValidated"]  as? Boolean ?: false
                )

                // Mettre à jour les vues sur le thread principal
                activity?.runOnUiThread {
                    tvTitleRef?.text = state.displayTitle
                    tvDescriptionRef?.text = buildString {
                        appendLine("Famille : ${state.family ?: "Non identifiée"}")
                        appendLine("Genre   : ${state.genre   ?: "Non identifié"}")
                        append(    "Espèce  : ${state.specie  ?: "Non identifiée"}")
                    }
                    refreshValidationStatus(tvStatusRef)
                    refreshValidateButton()
                    btnResumeCensusRef?.visibility =
                        if (state.showResumeCensus) View.VISIBLE else View.GONE
                }

                // Notifier le parent (MyProfile, Admin) pour qu'il recharge sa liste
                notifyParent()
            },
            onError = {
                // En cas d'erreur réseau, mise à jour partielle sans taxonomie
                state = state.copy(recordingStatus = true)
                activity?.runOnUiThread {
                    refreshValidationStatus(tvStatusRef)
                    btnResumeCensusRef?.visibility =
                        if (state.showResumeCensus) View.VISIBLE else View.GONE
                }
                notifyParent()
            }
        )
    }

    // ── Notification du parent ────────────────────────────────────────────────

    private fun notifyParent() {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply { putBoolean("census_updated", true) }
        )
    }

    // ── Taille de la popup ────────────────────────────────────────────────────

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dm = resources.displayMetrics
            window.setLayout((dm.widthPixels * 0.92f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────

    private fun refreshValidationStatus(tv: TextView?) {
        tv ?: return
        when {
            state.adminValidated -> {
                tv.text = "✓ Validé par un administrateur"
                tv.setTextColor(Color.parseColor("#4CAF50"))
            }
            !state.recordingStatus -> {
                tv.text = "● Recensement non terminé"
                tv.setTextColor(Color.parseColor("#F44336"))
            }
            else -> {
                tv.text = "○ Recensement non validé par un admin"
                tv.setTextColor(Color.parseColor("#FF9800"))
            }
        }
        tv.visibility = View.VISIBLE
    }

    private fun refreshValidateButton() {
        val btn = btnValidateRef ?: return
        if (state.adminValidated) {
            btn.text = "Invalider"
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
        } else {
            btn.text = "Valider"
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
        }
    }
}