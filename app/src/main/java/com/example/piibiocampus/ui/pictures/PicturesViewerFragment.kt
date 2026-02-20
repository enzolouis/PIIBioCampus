package com.example.piibiocampus.ui.photo

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
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
    private var btnValidateRef: Button? = null
    private var ivBadgeRef: ImageView?  = null

    companion object {
        private const val TAG = "PicturesViewerFragment"
        private const val ARG_STATE = "state"

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_pictures_viewer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivAuthorProfile  = view.findViewById<ImageView>(R.id.ivAuthorProfile)
        val tvTitle          = view.findViewById<TextView>(R.id.tvTitle)
        val ivValidatedBadge = view.findViewById<ImageView>(R.id.ivValidatedBadge)
        val photoView        = view.findViewById<PhotoView>(R.id.photoView)
        val tvDate           = view.findViewById<TextView>(R.id.tvDate)
        val tvDescription    = view.findViewById<TextView>(R.id.tvDescription)
        val btnResumeCensus  = view.findViewById<Button>(R.id.btnResumeCensus)
        val btnValidate      = view.findViewById<Button>(R.id.btnValidate)
        val btnBack          = view.findViewById<Button>(R.id.btnBack)
        val btnDelete        = view.findViewById<Button>(R.id.btnDelete)

        btnValidateRef = btnValidate
        ivBadgeRef     = ivValidatedBadge

        // ── DEBUG LOG ─────────────────────────────────────────────────────────
        android.util.Log.d("PicturesViewer", """
            ┌─── PhotoViewerState ───────────────────────────────
            │ pictureId           = '${state.pictureId}'
            │ imageUrl            = '${state.imageUrl}'
            │ authorId            = '${state.authorId}'
            │ adminValidated      = ${state.adminValidated}
            │ censusRef           = '${state.censusRef}'
            │ caller              = ${state.caller}
            │ family              = '${state.family}'
            │ genre               = '${state.genre}'
            │ specie              = '${state.specie}'
            │ timestamp           = '${state.timestamp}'
            │ imageBytes null ?   = ${state.imageBytes == null}
            ├─── Visibilité calculée ────────────────────────────
            │ showAuthorProfile   = ${state.showAuthorProfile}
            │ showResumeCensus    = ${state.showResumeCensus}
            │ showDelete          = ${state.showDelete}
            │ showValidate        = ${state.showValidate}
            │ showValidatedBadge  = ${state.showValidatedBadge}
            │ displayTitle        = '${state.displayTitle}'
            └────────────────────────────────────────────────────
        """.trimIndent())
        // ─────────────────────────────────────────────────────────────────────

        // --- Photo principale ---
        Picasso.get().load(state.imageUrl)
            .placeholder(R.drawable.photo_placeholder)
            .error(R.drawable.photo_placeholder)
            .into(photoView)

        // --- Photo de profil auteur ---
        if (state.showAuthorProfile && !state.authorProfilePictureUrl.isNullOrEmpty()) {
            ivAuthorProfile.visibility = View.VISIBLE
            Picasso.get().load(state.authorProfilePictureUrl)
                .placeholder(R.drawable.user_circle)
                .transform(CircleTransform())
                .into(ivAuthorProfile)
        } else {
            ivAuthorProfile.visibility = View.GONE
        }

        // --- Titre ---
        tvTitle.text = state.displayTitle

        // --- Badge validé ---
        ivValidatedBadge.visibility = if (state.showValidatedBadge) View.VISIBLE else View.GONE
        ivValidatedBadge.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Photo validée ✓")
                .setMessage("Cette photo a été vérifiée et validée par un administrateur de la plateforme.")
                .setPositiveButton("OK", null).show()
        }

        // --- Date ---
        tvDate.text = state.timestamp

        // --- Description ---
        tvDescription.text = buildString {
            appendLine("Famille : ${state.family ?: "Non identifiée"}")
            appendLine("Genre   : ${state.genre   ?: "Non identifié"}")
            append(    "Espèce  : ${state.specie  ?: "Non identifiée"}")
        }

        // --- Bouton "Reprendre le recensement" ---
        // showResumeCensus = true si adminValidated==false ET censusRef non vide
        // On ouvre CensusTreeActivity en mode UPDATE : pictureId + imageUrl suffisent,
        // pas besoin de télécharger les bytes.
        if (state.showResumeCensus) {
            btnResumeCensus.visibility = View.VISIBLE
            btnResumeCensus.text = "Reprendre le recensement"
            btnResumeCensus.setOnClickListener {
                val intent = Intent(requireContext(), CensusTreeActivity::class.java).apply {
                    putExtra("mode",          CensusMode.UPDATE.name)
                    putExtra("pictureId",     state.pictureId)
                    putExtra("imageUrl",      state.imageUrl)
                    // null ou "null" → initialNodeId absent → repart de la racine
                    val ref = state.censusRef?.takeIf { it.isNotEmpty() && it != "null" }
                    if (ref != null) putExtra("initialNodeId", ref)
                }
                startActivity(intent)
                dismiss()
            }
        } else {
            btnResumeCensus.visibility = View.GONE
        }

        // --- Bouton Valider / Invalider (admin) ---
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
                                btnResumeCensus.visibility =
                                    if (state.showResumeCensus) View.VISIBLE else View.GONE
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
        } else {
            btnValidate.visibility = View.GONE
        }

        // --- Retour ---
        btnBack.setOnClickListener { dismiss() }

        // --- Supprimer (double confirmation) ---
        if (state.showDelete) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Supprimer la photo")
                    .setMessage("Êtes-vous sûr de vouloir supprimer cette photo ?")
                    .setPositiveButton("Supprimer") { _, _ ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Confirmation finale")
                            .setMessage("Cette action est irréversible. Confirmer ?")
                            .setPositiveButton("Oui, supprimer") { _, _ ->
                                PictureDao.deletePicture(state.pictureId,
                                    onSuccess = {
                                        Toast.makeText(requireContext(), "Photo supprimée", Toast.LENGTH_SHORT).show()
                                        dismiss()
                                    },
                                    onError = { e ->
                                        Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            .setNegativeButton("Annuler", null).show()
                    }
                    .setNegativeButton("Annuler", null).show()
            }
        } else {
            btnDelete.visibility = View.GONE
        }
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

    private fun refreshValidateButton() {
        val btn   = btnValidateRef ?: return
        val badge = ivBadgeRef     ?: return
        if (state.adminValidated) {
            btn.text = "Invalider"
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
        } else {
            btn.text = "Valider"
            btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
        }
        badge.visibility = if (state.showValidatedBadge) View.VISIBLE else View.GONE
    }
}