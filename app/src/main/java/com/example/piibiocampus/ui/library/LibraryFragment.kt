package com.example.piibiocampus.ui.library

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.MainActivity
import com.example.piibiocampus.utils.setTopBarTitle
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText

class LibraryFragment : Fragment() {

    private val viewModel: LibraryViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar:  ProgressBar
    private lateinit var tvEmpty:      TextView
    private lateinit var tvCount:      TextView
    private lateinit var etSearch:     TextInputEditText
    private lateinit var chipCampus:   Chip
    private lateinit var chipReset:    Chip

    private lateinit var adapter: LibraryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_library, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTopBarTitle(R.string.bibliotheque)

        recyclerView = view.findViewById(R.id.recyclerViewLibrary)
        progressBar  = view.findViewById(R.id.progressBarLibrary)
        tvEmpty      = view.findViewById(R.id.tvLibraryEmpty)
        tvCount      = view.findViewById(R.id.tvLibraryCount)
        etSearch     = view.findViewById(R.id.etSearchLibrary)
        chipCampus   = view.findViewById(R.id.chipFilterCampusLib)
        chipReset    = view.findViewById(R.id.chipResetLib)

        setupRecyclerView()
        setupSearch()
        setupChips()
        observeViewModel()

        // Bouton retour système → retour vers MainActivity (comme MyProfileFragment)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
            }
        )

        viewModel.loadAll()
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.bibliotheque)
    }

    // ── RecyclerView ──────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = LibraryAdapter(emptyList()) { species ->
            SpeciesDetailDialogFragment.show(parentFragmentManager, species)
        }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    // ── Recherche en temps réel ───────────────────────────

    private fun setupSearch() {
        etSearch.addTextChangedListener { text ->
            val q = text.toString().trim()
            viewModel.searchQuery = q.ifBlank { null }
            viewModel.applyFilters()
        }
    }

    // ── Chips campus / reset ──────────────────────────────

    private fun setupChips() {
        chipCampus.setOnClickListener { showCampusDialog() }

        chipReset.setOnClickListener {
            viewModel.resetFilters()
            etSearch.setText("")
            chipCampus.text      = "Campus"
            chipCampus.isChecked = false
        }
    }

    // ── Observers ─────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.displayedSpecies.observe(viewLifecycleOwner) { list ->
            adapter.update(list)
            tvCount.text            = "${list.size} espèce(s)"
            tvEmpty.visibility      = if (list.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (list.isEmpty()) View.GONE   else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }
    }

    // ── Dialog campus ─────────────────────────────────────

    private fun showCampusDialog() {
        val campusList = viewModel.campusList.value ?: emptyList()
        if (campusList.isEmpty()) {
            chipCampus.isChecked = viewModel.filterCampusId != null  // ← remet l'état réel
            Toast.makeText(requireContext(), "Aucun campus disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val items   = campusList.map { it.name }.toTypedArray()
        val current = campusList.indexOfFirst {
            it.id == viewModel.filterCampusId || it.name == viewModel.filterCampusId
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrer par campus")
            .setSingleChoiceItems(items, current) { dialog, which ->
                val selected = campusList[which]
                viewModel.filterCampusId = selected.id.ifBlank { selected.name }
                chipCampus.text          = selected.name
                chipCampus.isChecked     = true
                viewModel.applyFilters()
                dialog.dismiss()
            }
            .setNegativeButton("Annuler") { _, _ ->
                // Remettre l'état réel : coché seulement si un campus est déjà sélectionné
                chipCampus.isChecked = viewModel.filterCampusId != null
            }
            .show()
    }
}