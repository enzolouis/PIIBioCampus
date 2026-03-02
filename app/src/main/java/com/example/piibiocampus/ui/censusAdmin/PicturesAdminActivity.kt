package com.example.piibiocampus.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.model.Campus
import com.example.piibiocampus.data.model.UserProfile
import com.example.piibiocampus.ui.photo.PhotoViewerState
import com.example.piibiocampus.ui.photo.PicturesViewerCaller
import com.example.piibiocampus.ui.photo.PicturesViewerFragment
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class PicturesAdminActivity : AppCompatActivity() {

    private val viewModel: PicturesAdminViewModel by viewModels { PicturesAdminViewModelFactory() }

    private lateinit var chipFilterUser: Chip
    private lateinit var chipFilterCampus: Chip
    private lateinit var chipFilterValidated: Chip
    private lateinit var chipFilterNotValidated: Chip
    private lateinit var chipFilterDate: Chip
    private lateinit var chipReset: Chip
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvResultCount: TextView

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_valide_pitures_admin)

        bindViews()
        setupAdapter()
        setupChips()
        observeViewModel()

        supportFragmentManager.setFragmentResultListener(
            PicturesViewerFragment.REQUEST_KEY,
            this
        ) { _, bundle ->

            val updated = bundle.getBoolean("census_updated", false)
            val deleted = bundle.getBoolean(PicturesViewerFragment.RESULT_DELETED, false)

            if (updated || deleted) {
                reloadData()
            }
        }

        viewModel.loadAll()
    }

    private fun bindViews() {
        chipFilterUser = findViewById(R.id.chipFilterUser)
        chipFilterCampus = findViewById(R.id.chipFilterCampus)
        chipFilterValidated = findViewById(R.id.chipFilterValidated)
        chipFilterNotValidated = findViewById(R.id.chipFilterNotValidated)
        chipFilterDate = findViewById(R.id.chipFilterDate)
        chipReset = findViewById(R.id.chipReset)
        recyclerView = findViewById(R.id.photosRecycler)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvResultCount = findViewById(R.id.tvResultCount)
    }

    private fun setupAdapter() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.filteredPictures.observe(this) { list ->
            photos.clear()
            photos.addAll(list)
            adapter.notifyDataSetChanged()
            tvResultCount.text = "${list.size} photo(s)"
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(this) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }
    }

    private fun setupChips() {
        chipFilterUser.setOnClickListener { showUserDialog() }
        chipFilterCampus.setOnClickListener { showCampusDialog() }
        chipFilterDate.setOnClickListener { showDateRangeDialog() }

        chipReset.setOnClickListener {
            viewModel.resetFilters()
            chipFilterUser.text = "Utilisateur"
            chipFilterCampus.text = "Campus"
            chipFilterDate.text = "Date"
        }
    }

    // ================= SEARCHABLE USER =================
    private fun showUserDialog() {
        val users = viewModel.allUsers.value ?: return
        showSearchableDialog(
            title = "Choisir un utilisateur",
            items = users,
            labelProvider = { it.name },
            onItemSelected = {
                viewModel.filterUserId = it.uid
                chipFilterUser.text = it.name
                viewModel.applyFilters()
            }
        )
    }

    // ================= SEARCHABLE CAMPUS =================
    private fun showCampusDialog() {
        val campusList = viewModel.campusList.value ?: return
        showSearchableDialog(
            title = "Choisir un campus",
            items = campusList,
            labelProvider = { it.name },
            onItemSelected = {
                viewModel.filterCampusId = it.name
                chipFilterCampus.text = it.name
                viewModel.applyFilters()
            }
        )
    }

    // ================= GENERIC SEARCH DIALOG =================
    private fun <T> showSearchableDialog(
        title: String,
        items: List<T>,
        labelProvider: (T) -> String,
        onItemSelected: (T) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_searchable_list, null)
        val editSearch = dialogView.findViewById<EditText>(R.id.editSearch)
        val listView = dialogView.findViewById<ListView>(R.id.listView)

        var filteredList = items.toMutableList()
        var adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredList.map { labelProvider(it) })
        listView.adapter = adapter

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase(Locale.getDefault())
                filteredList = items.filter { labelProvider(it).lowercase(Locale.getDefault()).contains(query) }.toMutableList()
                adapter = ArrayAdapter(this@PicturesAdminActivity, android.R.layout.simple_list_item_1, filteredList.map { labelProvider(it) })
                listView.adapter = adapter
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            onItemSelected(filteredList[position])
            dialog.dismiss()
        }

        dialog.show()
    }

    // ================= DATE RANGE WITH TITLES =================
    private fun showDateRangeDialog() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, y1, m1, d1 ->
            val from = Calendar.getInstance().apply { set(y1, m1, d1, 0, 0, 0) }

            DatePickerDialog(this, { _, y2, m2, d2 ->
                val to = Calendar.getInstance().apply { set(y2, m2, d2, 23, 59, 59) }

                viewModel.filterDateFrom = from
                viewModel.filterDateTo = to

                val fmt = SimpleDateFormat("dd/MM/yy", Locale.FRENCH)
                chipFilterDate.text = "${fmt.format(from.time)} → ${fmt.format(to.time)}"
                viewModel.applyFilters()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Sélectionner la date de fin")
            }.show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Sélectionner la date de début")
        }.show()
    }

    // ================= OPEN VIEWER =================
    private fun openViewer(photo: Map<String, Any>) {
        val loc = photo["location"] as? Map<*, *>
        val state = PhotoViewerState(
            imageUrl = photo["imageUrl"] as? String ?: "",
            family = photo["family"] as? String,
            genre = photo["genre"] as? String,
            specie = photo["specie"] as? String,
            timestamp = formatTimestamp(photo["timestamp"]),
            adminValidated = photo["adminValidated"] as? Boolean ?: false,
            pictureId = photo["id"] as? String ?: "",
            userRef = photo["userRef"] as? String ?: "",
            profilePictureUrl = photo["profilePictureUrl"] as? String,
            censusRef = photo["censusRef"] as? String,
            imageBytes = null,
            latitude = (loc?.get("latitude") as? Double) ?: 0.0,
            longitude = (loc?.get("longitude") as? Double) ?: 0.0,
            altitude = (loc?.get("altitude") as? Double) ?: 0.0,
            recordingStatus = photo["recordingStatus"] as? Boolean ?: false,
            caller = PicturesViewerCaller.ADMIN
        )
        PicturesViewerFragment.show(supportFragmentManager, state)
    }

    private fun formatTimestamp(timestamp: Any?): String = when (timestamp) {
        is com.google.firebase.Timestamp -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(timestamp.toDate())
        is Date -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(timestamp)
        else -> "Date inconnue"
    }

    private fun reloadData() {
        viewModel.loadAll()
    }

    // ================= ADAPTER =================
    inner class PhotoAdapter(private val items: List<Map<String, Any>>) :
        RecyclerView.Adapter<PhotoAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.photoItem)
            val recordingDot: View = view.findViewById(R.id.ivRecordingDot)
            val validatedDot: View = view.findViewById(R.id.ivValidatedBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val photo = items[position]
            val url = photo["imageUrl"] as? String ?: ""
            Picasso.get().load(url).placeholder(R.drawable.photo_placeholder).error(R.drawable.photo_placeholder).fit().centerCrop().into(holder.image)

            val adminValidated = photo["adminValidated"] as? Boolean ?: false
            val recordingStatus = photo["recordingStatus"] as? Boolean ?: false
            holder.validatedDot.visibility = if (adminValidated) View.VISIBLE else View.GONE
            holder.recordingDot.visibility = if (!adminValidated && !recordingStatus) View.VISIBLE else View.GONE

            holder.image.setOnClickListener { openViewer(photo) }
        }
    }
}