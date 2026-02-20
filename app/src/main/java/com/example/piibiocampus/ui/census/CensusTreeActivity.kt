package com.example.piibiocampus.ui.census

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.PictureDao
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.ui.MainActivity
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

/**
 * CREATE : nouvelle photo depuis PictureActivity → crée un document Firestore
 * UPDATE : reprise depuis MyProfile → met à jour le document existant sans re-upload
 */
enum class CensusMode { CREATE, UPDATE }

class CensusTreeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var previewThumbnail: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnCancel: Button
    private lateinit var btnStop: Button
    private lateinit var btnValidate: Button

    private val viewModel: CensusViewModel by viewModels { CensusViewModelFactory() }

    private var imageBytes: ByteArray? = null
    private var locationMeta: LocationMeta? = null

    private var mode: CensusMode = CensusMode.CREATE
    private var existingPictureId: String? = null
    private var existingImageUrl: String? = null

    private lateinit var adapter: CensusAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_census_tree)

        previewThumbnail = findViewById(R.id.previewThumbnail)
        tvHeaderTitle    = findViewById(R.id.tvHeaderTitle)
        recyclerView     = findViewById(R.id.recyclerView)
        btnBack          = findViewById(R.id.btnBackTree)
        btnCancel        = findViewById(R.id.btnCancel)
        btnStop          = findViewById(R.id.btnStop)
        btnValidate      = findViewById(R.id.btnValidate)

        mode = CensusMode.valueOf(intent.getStringExtra("mode") ?: CensusMode.CREATE.name)

        when (mode) {
            CensusMode.CREATE -> setupCreateMode()
            CensusMode.UPDATE -> setupUpdateMode()
        }

        setupRecyclerView()
        setupObservers()
        setupButtons()

        // Si censusRef est null ou "null" (string) → repart de la racine de l'arbre
        val initialNodeId = intent.getStringExtra("initialNodeId")
            ?.takeIf { it.isNotEmpty() && it != "null" }
        viewModel.refreshRoots(initialNodeId)
    }

    // ── Setup selon le mode ───────────────────────────────────────────────────

    private fun setupCreateMode() {
        imageBytes = intent.getByteArrayExtra("imageBytes")
        val lat = intent.getDoubleExtra("latitude",  Double.MIN_VALUE)
        val lon = intent.getDoubleExtra("longitude", Double.MIN_VALUE)
        val alt = intent.getDoubleExtra("altitude",  0.0)

        if (imageBytes == null || lat == Double.MIN_VALUE || lon == Double.MIN_VALUE) {
            Toast.makeText(this, "Données photo/location manquantes", Toast.LENGTH_LONG).show()
            finish(); return
        }
        locationMeta = LocationMeta(latitude = lat, longitude = lon, altitude = alt)

        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes!!.size)
        previewThumbnail.setImageBitmap(bmp)
        previewThumbnail.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_zoom, null)
            view.findViewById<PhotoView>(R.id.photoView).setImageBitmap(bmp)
            MaterialAlertDialogBuilder(this).setView(view)
                .setPositiveButton(android.R.string.ok, null).show()
        }
    }

    private fun setupUpdateMode() {
        existingPictureId = intent.getStringExtra("pictureId")
        existingImageUrl  = intent.getStringExtra("imageUrl")

        if (existingPictureId.isNullOrEmpty()) {
            Toast.makeText(this, "ID de la photo manquant", Toast.LENGTH_LONG).show()
            finish(); return
        }

        // Miniature depuis l'URL Firestore — pas de bytes nécessaires
        if (!existingImageUrl.isNullOrEmpty()) {
            Picasso.get().load(existingImageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .into(previewThumbnail)

            previewThumbnail.setOnClickListener {
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_zoom, null)
                Picasso.get().load(existingImageUrl).into(view.findViewById<PhotoView>(R.id.photoView))
                MaterialAlertDialogBuilder(this).setView(view)
                    .setPositiveButton(android.R.string.ok, null).show()
            }
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = CensusAdapter(
            items          = emptyList(),
            selectedNodeId = null,
            onItemClick    = { node, _ ->
                if (node.children.isNotEmpty()) viewModel.navigateTo(node)
                else viewModel.selectNode(node)
            },
            onInfoClick = { node -> showInfoDialog(node) }
        )
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.currentNodes.observe(this, Observer { nodes ->
            adapter.update(nodes, viewModel.selectedNodeId.value)
            updateHeaderTitle()
            val atSpecies = nodes.firstOrNull()?.type == CensusType.SPECIES
            btnValidate.visibility = if (atSpecies) View.VISIBLE else View.GONE
            btnBack.visibility = if (viewModel.canNavigateUp()) View.VISIBLE else View.GONE
        })

        viewModel.selectedNodeId.observe(this, Observer { selId ->
            adapter.update(viewModel.currentNodes.value ?: emptyList(), selId)
            val atSpecies = viewModel.currentNodes.value?.firstOrNull()?.type == CensusType.SPECIES
            btnValidate.isEnabled = (selId != null && atSpecies)
        })
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    private fun setupButtons() {
        btnBack.setOnClickListener {
            if (viewModel.canNavigateUp()) viewModel.navigateUp() else finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.canNavigateUp()) viewModel.navigateUp() else finish()
            }
        })

        btnCancel.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnStop.setOnClickListener {
            performSave(recordingStatus = false, censusRef = viewModel.selectedNodeId.value)
        }

        btnValidate.setOnClickListener {
            val idNode = viewModel.selectedNodeId.value
            if (idNode == null) {
                Toast.makeText(this, "Aucune espèce sélectionnée", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSave(recordingStatus = true, censusRef = idNode)
        }
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private fun performSave(recordingStatus: Boolean, censusRef: String?) {
        when (mode) {
            CensusMode.CREATE -> performCreate(recordingStatus, censusRef)
            CensusMode.UPDATE -> performUpdate(recordingStatus, censusRef)
        }
    }

    private fun performCreate(recordingStatus: Boolean, censusRef: String?) {
        val imgBytes = imageBytes
        val loc      = locationMeta
        if (imgBytes == null || loc == null) {
            Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show(); return
        }

        PictureDao.exportPictureFromBytes(
            context = this, imageBytes = imgBytes, location = loc,
            censusRef = censusRef, recordingStatus = recordingStatus, adminValidated = false,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Photo enregistrée", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java)); finish()
                }
            },
            onError = { e ->
                runOnUiThread { Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show() }
            }
        )
    }

    private fun performUpdate(recordingStatus: Boolean, censusRef: String?) {
        val pid = existingPictureId
        if (pid.isNullOrEmpty()) {
            Toast.makeText(this, "ID manquant", Toast.LENGTH_SHORT).show(); return
        }

        PictureDao.updatePictureCensus(
            pictureId = pid, censusRef = censusRef, recordingStatus = recordingStatus,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Recensement mis à jour", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java)); finish()
                }
            },
            onError = { e ->
                runOnUiThread { Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show() }
            }
        )
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun updateHeaderTitle() {
        val path = viewModel.currentPath()
        if (path.isEmpty()) { tvHeaderTitle.text = "Sélectionner un ordre"; return }
        val last = path.lastOrNull() ?: return
        val type = when (last.type) {
            CensusType.ORDER   -> "Ordre"
            CensusType.FAMILY  -> "Famille"
            CensusType.GENUS   -> "Genre"
            CensusType.SPECIES -> "Espèce"
        }
        tvHeaderTitle.text = "$type : ${last.name}"
    }

    private fun showInfoDialog(item: CensusNode) {
        val view    = LayoutInflater.from(this).inflate(R.layout.dialog_census_info, null)
        val iv      = view.findViewById<ImageView>(R.id.ivDialogImage)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDesc  = view.findViewById<TextView>(R.id.tvDescription)
        tvTitle.text = item.name
        tvDesc.text  = item.description.ifEmpty { listOf("Aucune description disponible") }.joinToString("\n\n")
        if (item.imageUrl.isNotBlank())
            Picasso.get().load(item.imageUrl).placeholder(R.drawable.ic_placeholder_image).into(iv)
        else iv.setImageResource(R.drawable.ic_placeholder_image)
        MaterialAlertDialogBuilder(this).setView(view).setPositiveButton(android.R.string.ok, null).show()
    }
}