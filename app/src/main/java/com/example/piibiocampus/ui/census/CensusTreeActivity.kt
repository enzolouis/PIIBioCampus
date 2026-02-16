package com.example.piibiocampus.ui.census

import PictureDao.exportPictureFromBytes
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.ui.MainActivity
import com.example.piibiocampus.ui.census.CensusNode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.github.chrisbanes.photoview.PhotoView

class CensusTreeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var previewThumbnail: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnStop: Button
    private lateinit var btnValidate: Button

    private val spanCount = 2
    private val spacingDp = 12

    private val viewModel: CensusViewModel by viewModels { CensusViewModelFactory() }

    // image passed from preview
    private var imageBytes: ByteArray? = null
    private var locationMeta: LocationMeta? = null

    private lateinit var adapter: CensusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_census_tree)

        previewThumbnail = findViewById(R.id.previewThumbnail)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        recyclerView = findViewById(R.id.recyclerView)
        btnCancel = findViewById(R.id.btnCancel)
        btnStop = findViewById(R.id.btnStop)
        btnValidate = findViewById(R.id.btnValidate)

        // Récupérer imageBytes + location depuis Intent
        imageBytes = intent.getByteArrayExtra("imageBytes")
        val lat = intent.getDoubleExtra("latitude", Double.MIN_VALUE)
        val lon = intent.getDoubleExtra("longitude", Double.MIN_VALUE)
        val alt = intent.getDoubleExtra("altitude", 0.0)
        if (imageBytes == null || lat == Double.MIN_VALUE || lon == Double.MIN_VALUE) {
            Toast.makeText(this, "Données photo/location manquantes", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        locationMeta = LocationMeta(latitude = lat, longitude = lon, altitude = alt)

        // afficher miniature
        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)
        previewThumbnail.setImageBitmap(bmp)

        // clic sur miniature -> dialog PhotoView fullscreen (PhotoView importé)
        previewThumbnail.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_zoom, null)
            val photoView = view.findViewById<PhotoView>(R.id.photoView)
            photoView.setImageBitmap(bmp)
            MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        // adapter initial vide
        adapter = CensusAdapter(emptyList(), selectedNodeId = null,
            onItemClick = { node, _ ->
                if (node.children.isNotEmpty()) {
                    // naviguer vers les enfants
                    viewModel.navigateTo(node)
                } else {
                    // feuille (espèce) -> sélectionner (surligner)
                    viewModel.selectNode(node)
                }
            },
            onInfoClick = { node ->
                showInfoDialog(node)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, spanCount)
        recyclerView.adapter = adapter

        // Observers
        viewModel.currentNodes.observe(this, Observer { nodes ->
            // Mettre à jour adapter avec la liste (selectedId sera appliqué via observer selectedNodeId)
            adapter.update(nodes, viewModel.selectedNodeId.value)
            // Mettre à jour header (ex. "Order: Passeriformes" ou "Select an order")
            updateHeaderTitle()
            // afficher/masquer bouton Valider suivant le niveau courant
            val atSpeciesLevel = nodes.firstOrNull()?.type == com.example.piibiocampus.ui.census.CensusType.SPECIES
            btnValidate.visibility = if (atSpeciesLevel) View.VISIBLE else View.GONE
            // enabled state will be updated by selectedNode observer
        })

        viewModel.selectedNodeId.observe(this, Observer { selId ->
            // Met à jour le surlignage
            adapter.update(viewModel.currentNodes.value ?: emptyList(), selId)
            // active/désactive bouton Valider si on est au niveau species
            val atSpeciesLevel = viewModel.currentNodes.value?.firstOrNull()?.type == com.example.piibiocampus.ui.census.CensusType.SPECIES
            btnValidate.isEnabled = (selId != null && atSpeciesLevel)
        })

        // bouton Annuler -> MainActivity
        btnCancel.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
            finish()
        }

        // bouton Stopper -> upload partial (recordingStatus = false)
        btnStop.setOnClickListener {
            val idNode = viewModel.selectedNodeId.value
            performUpload(recordingStatus = false, censusRef = idNode)
        }

        // bouton Valider -> upload with recordingStatus = true
        btnValidate.setOnClickListener {
            val idNode = viewModel.selectedNodeId.value
            if (idNode == null) {
                Toast.makeText(this, "Aucune espèce sélectionnée", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performUpload(recordingStatus = true, censusRef = idNode)
        }

        // Gestion du back gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.canNavigateUp()) {
                    viewModel.navigateUp()
                } else {
                    finish()
                }
            }
        })

        // initial load
        viewModel.refreshRoots()
    }

    private fun updateHeaderTitle() {
        val path = viewModel.currentPath()
        if (path.isEmpty()) {
            tvHeaderTitle.text = "Select an order"
            return
        }
        val last = path.lastOrNull()
        if (last != null) {
            val typeName = when (last.type) {
                com.example.piibiocampus.ui.census.CensusType.ORDER -> "Order"
                com.example.piibiocampus.ui.census.CensusType.FAMILY -> "Family"
                com.example.piibiocampus.ui.census.CensusType.GENUS -> "Genus"
                com.example.piibiocampus.ui.census.CensusType.SPECIES -> "Species"
            }
            tvHeaderTitle.text = "$typeName: ${last.name}"
        } else {
            tvHeaderTitle.text = "Select an order"
        }
    }

    private fun showInfoDialog(item: CensusNode) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_census_info, null)
        val iv = view.findViewById<ImageView>(R.id.ivDialogImage)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvDescription)

        tvTitle.text = item.name
        tvDesc.text = if (item.description.isEmpty()) "Aucune description disponible" else item.description.joinToString("\n\n")

        if (item.imageUrl.isNotBlank()) {
            Picasso.get().load(item.imageUrl).placeholder(R.drawable.ic_placeholder_image).into(iv)
        } else {
            iv.setImageResource(R.drawable.ic_placeholder_image)
        }

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun performUpload(recordingStatus: Boolean, censusRef: String?) {
        val imgBytes = imageBytes
        val loc = locationMeta
        if (imgBytes == null || loc == null) {
            Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show()
            return
        }

        // speciesRef not used: pass null
        exportPictureFromBytes(
            context = this,
            imageBytes = imgBytes,
            location = loc,
            censusRef = censusRef,
            userRef = null,
            speciesRef = null,
            recordingStatus = recordingStatus,
            adminValidated = false,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Upload effectué", Toast.LENGTH_SHORT).show()
                    val i = Intent(this, MainActivity::class.java)
                    startActivity(i)
                    finish()
                }
            },
            onError = { e ->
                runOnUiThread {
                    Toast.makeText(this, "Erreur upload: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}