package com.fneb.piibiocampus.ui.census

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.LocationMeta
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.ui.MainActivity
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

enum class CensusMode { CREATE, UPDATE }

class CensusTreeActivity : BaseActivity() {

    private lateinit var recyclerView:     RecyclerView
    private lateinit var previewThumbnail: ImageView
    private lateinit var tvHeaderTitle:    TextView
    private lateinit var btnBack:          ImageButton
    private lateinit var btnCancel:        Button
    private lateinit var btnStop:          Button
    private lateinit var btnValidate:      Button
    private lateinit var tvEmptyLevel:     TextView
    private var progressBar: View? = null

    private val viewModel: CensusViewModel by viewModels { CensusViewModelFactory() }

    private var imageBytes:        ByteArray? = null
    private var locationMeta:      LocationMeta? = null
    private var mode:              CensusMode = CensusMode.CREATE
    private var existingPictureId: String? = null
    private var existingImageUrl:  String? = null
    private var zoomDialog:        android.app.Dialog? = null
    private var callerName:        String = PicturesViewerCaller.MAP.name

    private lateinit var adapter: CensusAdapter

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        zoomDialog?.dismiss()
        zoomDialog = null
    }

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
        tvEmptyLevel     = findViewById(R.id.tvEmptyLevel)
        progressBar      = findViewById(R.id.progressBar)

        mode       = CensusMode.valueOf(intent.getStringExtra("mode") ?: CensusMode.CREATE.name)
        callerName = intent.getStringExtra("caller") ?: PicturesViewerCaller.MAP.name

        when (mode) {
            CensusMode.CREATE -> setupCreateMode()
            CensusMode.UPDATE -> setupUpdateMode()
        }

        setupRecyclerView()
        setupObservers()
        setupButtons()

        val initialNodeId = intent.getStringExtra("initialNodeId")
            ?.takeIf { it.isNotEmpty() && it != "null" }
        viewModel.refreshRoots(initialNodeId)
    }

    // ── Setup modes ───────────────────────────────────────────────────────────

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
        previewThumbnail.setOnClickListener { showZoomDialog(bmp) }
    }

    private fun setupUpdateMode() {
        existingPictureId = intent.getStringExtra("pictureId")
        existingImageUrl  = intent.getStringExtra("imageUrl")

        if (existingPictureId.isNullOrEmpty()) {
            Toast.makeText(this, "ID de la photo manquant", Toast.LENGTH_LONG).show()
            finish(); return
        }

        if (!existingImageUrl.isNullOrEmpty()) {
            Picasso.get().load(existingImageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .into(previewThumbnail)
            previewThumbnail.setOnClickListener {
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_zoom, null)
                Picasso.get().load(existingImageUrl).into(view.findViewById<PhotoView>(R.id.photoView))
                zoomDialog = MaterialAlertDialogBuilder(this).setView(view)
                    .setPositiveButton(android.R.string.ok) { _, _ -> zoomDialog = null }.create()
                zoomDialog?.show()
            }
        }
    }

    private fun showZoomDialog(bmp: android.graphics.Bitmap) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_zoom, null)
        view.findViewById<PhotoView>(R.id.photoView).setImageBitmap(bmp)
        zoomDialog = MaterialAlertDialogBuilder(this).setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ -> zoomDialog = null }.create()
        zoomDialog?.show()
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = CensusAdapter(
            items          = emptyList(),
            selectedNodeId = null,
            onItemClick    = { node, _ ->
                when {
                    node.type == CensusType.SPECIES -> viewModel.selectNode(node)
                    else                            -> viewModel.navigateTo(node)
                }
            },
            onInfoClick = { node -> showInfoDialog(node) }
        )
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {

        // Chargement de l'arbre
        viewModel.treeState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Error   -> {
                    showLoading(false)
                    showError(state.exception)
                }
                // Success : les nodes arrivent via currentNodes ci-dessous
                else -> showLoading(false)
            }
        }

        // Résultat de la sauvegarde photo
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    onSaveSuccess()
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                }
                else -> Unit
            }
        }

        // Nœuds courants (navigation)
        viewModel.currentNodes.observe(this) { nodes ->
            adapter.update(nodes, viewModel.selectedNodeId.value)
            updateHeaderTitle()

            val atSpecies  = nodes.firstOrNull()?.type == CensusType.SPECIES
            val levelEmpty = nodes.isEmpty() && viewModel.canNavigateUp()

            btnValidate.visibility = if (atSpecies) View.VISIBLE else View.GONE
            btnBack.visibility     = if (viewModel.canNavigateUp()) View.VISIBLE else View.GONE

            if (levelEmpty) {
                recyclerView.visibility = View.GONE
                tvEmptyLevel.visibility = View.VISIBLE
                btnValidate.visibility  = View.GONE
            } else {
                recyclerView.visibility = View.VISIBLE
                tvEmptyLevel.visibility = View.GONE
            }
        }

        // Sélection d'espèce
        viewModel.selectedNodeId.observe(this) { selId ->
            adapter.update(viewModel.currentNodes.value ?: emptyList(), selId)
            val atSpecies = viewModel.currentNodes.value?.firstOrNull()?.type == CensusType.SPECIES
            btnValidate.isEnabled = (selId != null && atSpecies)
        }
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

        btnCancel.setOnClickListener { finish() }

        btnStop.setOnClickListener {
            performSave(recordingStatus = false, censusRef = viewModel.stopCensusRef())
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

    /**
     * Délègue entièrement la sauvegarde au ViewModel.
     * L'Activity n'appelle plus aucun DAO directement.
     */
    private fun performSave(recordingStatus: Boolean, censusRef: String?) {
        when (mode) {
            CensusMode.CREATE -> {
                val imgBytes = imageBytes ?: run {
                    Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show(); return
                }
                val loc = locationMeta ?: run {
                    Toast.makeText(this, "Données manquantes", Toast.LENGTH_SHORT).show(); return
                }
                viewModel.createPicture(this, imgBytes, loc, censusRef, recordingStatus)
            }
            CensusMode.UPDATE -> {
                val pid = existingPictureId ?: run {
                    Toast.makeText(this, "ID manquant", Toast.LENGTH_SHORT).show(); return
                }
                viewModel.updatePicture(pid, censusRef, recordingStatus)
            }
        }
    }

    /** Appelé quand saveState passe à Success. */
    private fun onSaveSuccess() {
        when (mode) {
            CensusMode.CREATE -> {
                Toast.makeText(this, "Photo enregistrée", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigateTo", when (callerName) {
                        PicturesViewerCaller.MY_PROFILE.name -> "myProfile"
                        else                                  -> "map"
                    })
                }
                startActivity(intent)
                finish()
            }
            CensusMode.UPDATE -> {
                Toast.makeText(this, "Recensement mis à jour", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun showLoading(visible: Boolean) {
        progressBar?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun showError(exception: com.fneb.piibiocampus.data.error.AppException) {
        Snackbar.make(findViewById(android.R.id.content), exception.userMessage, Snackbar.LENGTH_LONG).show()
    }

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
        else
            iv.setImageResource(R.drawable.ic_placeholder_image)
        MaterialAlertDialogBuilder(this).setView(view).setPositiveButton(android.R.string.ok, null).show()
    }
}