package com.fneb.piibiocampus.ui.admin

import android.app.DatePickerDialog
import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportDataActivity : AppCompatActivity() {

    private val viewModel: ExportDataViewModel by viewModels { ExportDataViewModelFactory() }

    private lateinit var chipFilterCampus:     Chip
    private lateinit var chipFilterDate:       Chip
    private lateinit var chipFilterValidation: Chip
    private lateinit var chipReset:            Chip
    private lateinit var btnExportCsv:         MaterialButton
    private lateinit var progressBar:          ProgressBar
    private lateinit var tvResultCount:        TextView
    private lateinit var tvEmpty:              TextView
    private lateinit var previewRecycler:      RecyclerView

    private lateinit var previewAdapter: PreviewRowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_data)

        bindViews()
        setupPreviewRecycler()
        setupChips()
        observeViewModel()
        setTopBarTitle("Exportation des données")

        ViewCompat.setOnApplyWindowInsetsListener(btnExportCsv) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (20 * resources.displayMetrics.density).toInt()
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                systemBars.bottom + extraPadding
            insets
        }

        // Chip validation initialisé sur ONLY_VALIDATED par défaut
        updateValidationChipLabel()

        // Chargement — applyFilters() sera appelé automatiquement quand isLoading passe à false
        viewModel.loadAll()
    }

    private fun bindViews() {
        chipFilterCampus     = findViewById(R.id.chipFilterCampus)
        chipFilterDate       = findViewById(R.id.chipFilterDate)
        chipFilterValidation = findViewById(R.id.chipFilterValidation)
        chipReset            = findViewById(R.id.chipReset)
        btnExportCsv         = findViewById(R.id.btnExportCsv)
        progressBar          = findViewById(R.id.progressBar)
        tvResultCount        = findViewById(R.id.tvResultCount)
        tvEmpty              = findViewById(R.id.tvEmpty)
        previewRecycler      = findViewById(R.id.previewRecycler)
    }

    private fun setupPreviewRecycler() {
        previewAdapter = PreviewRowAdapter()
        previewRecycler.layoutManager = LinearLayoutManager(this)
        previewRecycler.adapter = previewAdapter
    }

    private fun observeViewModel() {
        var wasLoading = false
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnExportCsv.isEnabled = !loading
            // Applique le filtre "Validé uniquement" dès que le chargement initial est terminé
            if (wasLoading && !loading) {
                viewModel.applyFilters()
            }
            wasLoading = loading
        }

        viewModel.filteredCount.observe(this) { count ->
            tvResultCount.text     = "$count photo(s)"
            tvEmpty.visibility     = if (count == 0) View.VISIBLE else View.GONE
            btnExportCsv.isEnabled = count > 0
        }

        viewModel.previewRows.observe(this) { rows ->
            previewAdapter.submitList(rows)
        }

        viewModel.error.observe(this) { msg ->
            msg ?: return@observe
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupChips() {
        chipFilterCampus.setOnClickListener { showCampusDialog() }
        chipFilterDate.setOnClickListener   { showDateRangeDialog() }

        chipFilterValidation.setOnClickListener {
            viewModel.filterValidation = when (viewModel.filterValidation) {
                ValidationFilter.ONLY_VALIDATED    -> ValidationFilter.VALIDATED_AND_NOT
                ValidationFilter.VALIDATED_AND_NOT -> ValidationFilter.ONLY_VALIDATED
            }
            updateValidationChipLabel()
            viewModel.applyFilters()
        }

        chipReset.setOnClickListener {
            viewModel.resetFilters()
            chipFilterCampus.text      = "Campus"; chipFilterCampus.isChecked = false
            chipFilterDate.text        = "Date";   chipFilterDate.isChecked   = false
            updateValidationChipLabel()
        }

        btnExportCsv.setOnClickListener { exportCsv() }
    }

    private fun updateValidationChipLabel() {
        chipFilterValidation.isChecked = true
        chipFilterValidation.text = when (viewModel.filterValidation) {
            ValidationFilter.ONLY_VALIDATED    -> "Validé uniquement"
            ValidationFilter.VALIDATED_AND_NOT -> "Validé + Non validé"
        }
    }

    private fun showCampusDialog() {
        val campusList = viewModel.campusList.value ?: return

        data class CampusItem(val name: String, val id: String)
        val items = listOf(CampusItem("Hors campus", "HORS_CAMPUS")) +
                campusList.map { CampusItem(it.name, it.name) }

        showSearchableDialog(
            title          = "Choisir un campus",
            items          = items,
            labelProvider  = { it.name },
            onItemSelected = {
                viewModel.filterCampusId   = it.id
                chipFilterCampus.text      = it.name
                chipFilterCampus.isChecked = true
                viewModel.applyFilters()
            }
        )
    }

    private fun <T> showSearchableDialog(
        title: String,
        items: List<T>,
        labelProvider: (T) -> String,
        onItemSelected: (T) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_searchable_list, null)
        val editSearch = dialogView.findViewById<EditText>(R.id.editSearch)
        val listView   = dialogView.findViewById<ListView>(R.id.listView)

        var filteredList = items.toMutableList()
        var listAdapter  = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            filteredList.map { labelProvider(it) })
        listView.adapter = listAdapter

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().lowercase(Locale.getDefault())
                filteredList = items.filter {
                    labelProvider(it).lowercase(Locale.getDefault()).contains(q)
                }.toMutableList()
                listAdapter = ArrayAdapter(this@ExportDataActivity,
                    android.R.layout.simple_list_item_1,
                    filteredList.map { labelProvider(it) })
                listView.adapter = listAdapter
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(this).setTitle(title).setView(dialogView).create()

        listView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(filteredList[position])
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDateRangeDialog() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, y1, m1, d1 ->
            val from = Calendar.getInstance().apply { set(y1, m1, d1, 0, 0, 0) }
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val to = Calendar.getInstance().apply { set(y2, m2, d2, 23, 59, 59) }
                viewModel.filterDateFrom   = from
                viewModel.filterDateTo     = to
                val fmt = SimpleDateFormat("dd/MM/yy", Locale.FRENCH)
                chipFilterDate.text        = "${fmt.format(from.time)} → ${fmt.format(to.time)}"
                chipFilterDate.isChecked   = true
                viewModel.applyFilters()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                .apply { setTitle("Date de fin") }.show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            .apply { setTitle("Date de début") }.show()
    }

    private fun exportCsv() {
        val count = viewModel.filteredCount.value ?: 0
        if (count == 0) {
            Toast.makeText(this, "Aucune photo à exporter", Toast.LENGTH_SHORT).show()
            return
        }

        val csvContent = viewModel.buildCsvContent()
        val fileName   = "export_photos_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRENCH).format(Date())
        }.csv"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Impossible de créer le fichier")
                contentResolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray()) }
            } else {
                val dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                FileOutputStream(File(dir, fileName)).use { it.write(csvContent.toByteArray()) }
            }
            Toast.makeText(this, "CSV exporté : $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur export : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    inner class PreviewRowAdapter : RecyclerView.Adapter<PreviewRowAdapter.VH>() {

        private var rows: List<ExportRow> = emptyList()

        fun submitList(list: List<ExportRow>) {
            rows = list
            notifyDataSetChanged()
        }

        inner class VH(val container: LinearLayout) : RecyclerView.ViewHolder(container)

        override fun getItemCount() = rows.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val density = parent.context.resources.displayMetrics.density
            fun dp(value: Int) = (value * density).toInt()

            val colWidthsDp = intArrayOf(76, 80, 100, 100, 130, 80, 110, 110, 110, 110)

            val rowLayout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32))
            }

            colWidthsDp.forEachIndexed { index, widthDp ->
                if (index > 0) {
                    rowLayout.addView(View(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(Color.parseColor("#CCCCCC"))
                    })
                }
                rowLayout.addView(TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.MATCH_PARENT)
                    gravity      = Gravity.CENTER
                    textSize     = 11f
                    maxLines     = 1
                    ellipsize    = TextUtils.TruncateAt.END
                    setPadding(dp(4), 0, dp(4), 0)
                })
            }

            return VH(rowLayout)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]

            holder.container.setBackgroundColor(
                if (position % 2 == 0) Color.WHITE else Color.parseColor("#F5F5F5")
            )

            val cells = (0 until holder.container.childCount)
                .map { holder.container.getChildAt(it) }
                .filterIsInstance<TextView>()

            if (cells.size < 10) return

            cells[0].apply {
                text = if (row.adminValidated) "✓" else "✗"
                setTextColor(if (row.adminValidated) Color.parseColor("#2E7D32") else Color.parseColor("#C62828"))
            }
            cells[1].text = "%.1f m".format(row.altitude)
            cells[2].text = "%.5f".format(row.longitude)
            cells[3].text = "%.5f".format(row.latitude)
            cells[4].text = row.date
            cells[5].text = row.maxLevel.ifEmpty { "—" }
            cells[6].text = row.ordre.ifEmpty   { "—" }
            cells[7].text = row.famille.ifEmpty { "—" }
            cells[8].text = row.genre.ifEmpty   { "—" }
            cells[9].text = row.espece.ifEmpty  { "—" }
        }
    }
}