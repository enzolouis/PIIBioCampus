package com.fneb.piibiocampus.ui.census

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CensusEditorActivity : AppCompatActivity() {

    private val viewModel: CensusEditorViewModel by viewModels()

    private lateinit var recyclerView:   RecyclerView
    private lateinit var tvHeaderTitle:  TextView
    private lateinit var btnBack:        ImageButton
    private lateinit var progressBar:    ProgressBar
    private lateinit var tvEmpty:        TextView

    private lateinit var adapter: CensusEditorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_census_editor)

        recyclerView  = findViewById(R.id.recyclerView)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        btnBack       = findViewById(R.id.btnBackTree)
        progressBar   = findViewById(R.id.progressBar)
        tvEmpty       = findViewById(R.id.tvEmpty)

        setupRecyclerView()
        setupButtons()
        observeViewModel()
        setTopBarTitle(getString(R.string.arbre_de_recensement))
        viewModel.loadTree()
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = CensusEditorAdapter(
            items      = emptyList(),
            onNavigate = { node -> viewModel.navigateTo(node) },
            onEdit     = { node -> openEditDialog(node) },
            onDelete   = { node -> confirmDelete(node) },
            onAdd      = { openAddDialog() }
        )
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
        recyclerView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (20 * resources.displayMetrics.density).toInt()
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom + extraPadding
            )
            insets
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.currentNodes.observe(this) { nodes ->
            adapter.update(nodes)
            updateHeaderTitle()
            btnBack.visibility = if (viewModel.canNavigateUp()) View.VISIBLE else View.GONE
            tvEmpty.visibility = if (nodes.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { msg ->
            msg ?: return@observe
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    private fun setupButtons() {
        btnBack.setOnClickListener {
            if (viewModel.canNavigateUp()) viewModel.navigateUp()
            else finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.canNavigateUp()) viewModel.navigateUp()
                else finish()
            }
        })
    }

    // ── Dialogues ─────────────────────────────────────────────────────────────

    private fun openAddDialog() {
        CensusNodeEditorDialogFragment.show(
            fm     = supportFragmentManager,
            node   = null,
            type   = viewModel.childTypeForCurrentLevel()
        ) { name, description, imageUrl ->
            viewModel.addNode(name, description, imageUrl)
        }
    }

    private fun openEditDialog(node: CensusNode) {
        CensusNodeEditorDialogFragment.show(
            fm   = supportFragmentManager,
            node = node,
            type = node.type
        ) { name, description, imageUrl ->
            viewModel.updateNode(node, name, description, imageUrl)
        }
    }

    private fun confirmDelete(node: CensusNode) {
        val descendantCount = viewModel.countDescendants(node)
        val message = buildString {
            append("Supprimer « ${node.name} » ?")
            if (descendantCount > 0) {
                append("\n\nCette action supprimera également tout les enfants de ce noeud.")
            }
            append("\n\nCette action est irréversible.")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmer la suppression")
            .setMessage(message)
            .setPositiveButton("Supprimer") { _, _ -> viewModel.deleteNode(node) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ── En-tête ───────────────────────────────────────────────────────────────

    private fun updateHeaderTitle() {
        val path = viewModel.currentPath()
        tvHeaderTitle.text = when {
            path.isEmpty() -> "Arbre de recensement — Ordres"
            else -> {
                val last = path.last()
                val currentLevel = when (last.type) {
                    CensusType.ORDER   -> "Ordre : ${last.name}"
                    CensusType.FAMILY  -> "Famille : ${last.name}"
                    CensusType.GENUS   -> "Genre : ${last.name}"
                    CensusType.SPECIES -> "Espèce : ${last.name}"
                }
                val nextLevel = when (last.type) {
                    CensusType.ORDER   -> "Familles"
                    CensusType.FAMILY  -> "Genres"
                    CensusType.GENUS   -> "Espèces"
                    CensusType.SPECIES -> ""
                }
                if (nextLevel.isNotEmpty()) "$currentLevel — $nextLevel"
                else currentLevel
            }
        }
    }
}