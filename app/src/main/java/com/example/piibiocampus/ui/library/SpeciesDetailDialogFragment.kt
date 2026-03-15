package com.example.piibiocampus.ui.library

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.piibiocampus.R
import com.squareup.picasso.Picasso

/**
 * Popup de détail d'une espèce dans la bibliothèque.
 * Affiche l'image, la hiérarchie taxonomique complète, la description
 * et le statut de recensement de l'utilisateur·ice.
 *
 * Inspiré de PicturesViewerFragment mais entièrement dédié à la bibliothèque.
 */
class SpeciesDetailDialogFragment : DialogFragment() {

    private lateinit var species: SpeciesItem

    companion object {
        private const val TAG = "SpeciesDetailDialog"

        fun show(fm: FragmentManager, species: SpeciesItem) {
            SpeciesDetailDialogFragment().also {
                it.species = species
            }.show(fm, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogPopup)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_species_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivSpecies       = view.findViewById<ImageView>(R.id.ivSpeciesImage)
        val tvName          = view.findViewById<TextView>(R.id.tvSpeciesName)
        val tvTaxonomy      = view.findViewById<TextView>(R.id.tvTaxonomyPath)
        val tvDescription   = view.findViewById<TextView>(R.id.tvSpeciesDescription)
        val tvRecordStatus  = view.findViewById<TextView>(R.id.tvRecordStatus)
        val tvRecordCount   = view.findViewById<TextView>(R.id.tvRecordCount)
        val btnClose        = view.findViewById<Button>(R.id.btnCloseSpecies)

        // ── Image ──────────────────────────────────────────
        val imageUrl = species.taxonomyImageUrl
        if (imageUrl.isNotBlank()) {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .into(ivSpecies)
        } else {
            ivSpecies.setImageResource(R.drawable.ic_placeholder_image)
        }

        // ── Nom ────────────────────────────────────────────
        tvName.text = species.name

        // ── Hiérarchie taxonomique ─────────────────────────
        tvTaxonomy.text = buildString {
            appendLine("Ordre   : ${species.orderName}")
            appendLine("Famille : ${species.familyName}")
            append(    "Genre   : ${species.genusName}")
        }

        // ── Description ────────────────────────────────────
        if (species.description.isNotEmpty()) {
            tvDescription.text = species.description.joinToString("\n\n")
            tvDescription.visibility = View.VISIBLE
        } else {
            tvDescription.text = "Aucune description disponible."
            tvDescription.visibility = View.VISIBLE
        }

        // ── Statut recensement ─────────────────────────────
        if (species.isRecordedByUser) {
            tvRecordStatus.text = "✓ Vous avez recensé cette espèce"
            tvRecordStatus.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvRecordStatus.text = "○ Espèce non recensée par vous"
            tvRecordStatus.setTextColor(Color.parseColor("#FF9800"))
        }

        // ── Compteur global ────────────────────────────────
        tvRecordCount.text = when (species.totalRecordingsCount) {
            0    -> "Aucun recensement dans l'application"
            1    -> "1 recensement dans l'application"
            else -> "${species.totalRecordingsCount} recensements dans l'application"
        }

        // ── Fermeture ──────────────────────────────────────
        btnClose.setOnClickListener { dismiss() }
    }

    // ── Taille de la popup ────────────────────────────────

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dm = resources.displayMetrics
            window.setLayout(
                (dm.widthPixels * 0.92f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}
