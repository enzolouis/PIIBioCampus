package com.example.piibiocampus.ui.census

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class CensusTreeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val spanCount = 2
    private val spacingDp = 12

    private val viewModel: CensusViewModel by viewModels { CensusViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_census_tree)

        recyclerView = findViewById(R.id.recyclerView)

        // GridLayoutManager avec dernier élément centré si seul sur la dernière ligne
        val glm = GridLayoutManager(this, spanCount)
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val itemCount = recyclerView.adapter?.itemCount ?: 0
                return if (position == itemCount - 1 && itemCount % spanCount == 1) spanCount else 1
            }
        }
        recyclerView.layoutManager = glm

        val spacingPx = (spacingDp * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, spacingPx, true))
        recyclerView.clipToPadding = false

        // window insets pour bottom nav
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bottom = insets.systemWindowInsetBottom
            recyclerView.updatePadding(bottom = bottom + recyclerView.paddingBottom)
            insets
        }

        // Observe les nodes courants
        viewModel.currentNodes.observe(this, Observer { nodes ->
            val adapter = CensusAdapter(nodes,
                onItemClick = { node, _ ->
                    if (node.children.isNotEmpty()) {
                        viewModel.navigateTo(node)
                    } else {
                        showInfoDialog(node)
                    }
                },
                onInfoClick = { node ->
                    showInfoDialog(node)
                }
            )
            recyclerView.adapter = adapter
        })

        // Gestion du back avec OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.canNavigateUp()) {
                    viewModel.navigateUp() // revient au niveau précédent dans l'arbre
                } else {
                    finish() // ferme l'activité si on est à la racine
                }
            }
        })

        // rafraîchir les données
        viewModel.refreshRoots()
    }

    private fun showInfoDialog(item: CensusNode) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_census_info, null)
        val iv = view.findViewById<ImageView>(R.id.ivDialogImage)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvDescription)

        tvTitle.text = item.name
        tvDesc.text = if (item.description.isEmpty()) "(Aucune description)" else item.description.joinToString("\n\n")

        if (item.imageUrl.isNotBlank()) {
            Picasso.get()
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .into(iv)
        } else {
            iv.setImageResource(R.drawable.ic_placeholder_image)
        }

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
