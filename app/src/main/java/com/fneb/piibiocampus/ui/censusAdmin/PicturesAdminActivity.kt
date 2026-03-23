package com.fneb.piibiocampus.ui.admin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.graphics.Color
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
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.utils.setTopBarTitle
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
    private lateinit var btnSortOrder: ImageButton
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
        setupSortButton()
        observeViewModel()

        supportFragmentManager.setFragmentResultListener(
            PicturesViewerFragment.REQUEST_KEY, this
        ) { _, bundle ->
            val validatedId    = bundle.getString("validated_picture_id")
            val validatedValue = bundle.getBoolean("validated_value", false)
            val deletedId      = bundle.getString("deleted_picture_id")
            val updated        = bundle.getBoolean("census_updated", false)

            when {
                // Validation / invalidation : mise à jour en mémoire, sans appel réseau
                validatedId != null -> viewModel.updateValidationInPlace(validatedId, validatedValue)
                // Suppression : animation puis retrait en mémoire
                deletedId != null   -> animateDeleteAndRemove(deletedId)
                // Retour de recensement : rechargement complet
                updated             -> viewModel.loadAll()
            }
        }

        setTopBarTitle("Gestion des recensements")

        viewModel.loadAll()
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindViews() {
        chipFilterUser         = findViewById(R.id.chipFilterUser)
        chipFilterCampus       = findViewById(R.id.chipFilterCampus)
        chipFilterValidated    = findViewById(R.id.chipFilterValidated)
        chipFilterNotValidated = findViewById(R.id.chipFilterNotValidated)
        chipFilterDate         = findViewById(R.id.chipFilterDate)
        chipReset              = findViewById(R.id.chipReset)
        btnSortOrder           = findViewById(R.id.btnSortOrder)
        recyclerView           = findViewById(R.id.photosRecycler)
        progressBar            = findViewById(R.id.progressBar)
        tvEmpty                = findViewById(R.id.tvEmpty)
        tvResultCount          = findViewById(R.id.tvResultCount)
    }

    private fun setupAdapter() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.filteredPictures.observe(this) { list ->
            photos.clear()
            photos.addAll(list)
            adapter.notifyDataSetChanged()
            tvResultCount.text = "${list.size} photo(s)"
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Met à jour l'icône du bouton quand l'ordre change
        viewModel.sortOrder.observe(this) { order ->
            btnSortOrder.setImageResource(
                if (order == SortOrder.DESC) R.drawable.ic_sort_desc
                else                         R.drawable.ic_sort_asc
            )
        }
    }

    // ── Animation suppression ─────────────────────────────────────────────────

    private fun animateDeleteAndRemove(pictureId: String) {
        val position = photos.indexOfFirst { it["id"] == pictureId }

        // Si l'item n'est pas visible (hors écran ou déjà absent), on supprime directement
        val viewHolder = if (position >= 0) recyclerView.findViewHolderForAdapterPosition(position) else null
        if (viewHolder == null) {
            viewModel.deleteInPlace(pictureId)
            return
        }

        val itemView = viewHolder.itemView

        // Flash rouge qui revient transparent
        val colorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.parseColor("#FFCDD2"),
            Color.TRANSPARENT
        ).apply {
            duration = 700L
            addUpdateListener { itemView.setBackgroundColor(it.animatedValue as Int) }
        }

        // Fondu sortant
        val fadeOut = ObjectAnimator.ofFloat(itemView, "alpha", 1f, 0f).apply {
            duration = 400L
            startDelay = 400L
        }

        AnimatorSet().apply {
            playTogether(colorAnim, fadeOut)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Réinitialiser l'état visuel de la vue avant qu'elle soit recyclée
                    itemView.alpha = 1f
                    itemView.setBackgroundColor(Color.TRANSPARENT)
                    // Mettre à jour le ViewModel → l'observer rafraîchit la liste
                    viewModel.deleteInPlace(pictureId)
                }
            })
            start()
        }
    }

    // ── Bouton tri ────────────────────────────────────────────────────────────

    private fun setupSortButton() {
        btnSortOrder.setOnClickListener {
            viewModel.toggleSortOrder()
        }
    }

    // ── Chips ─────────────────────────────────────────────────────────────────

    private fun setupChips() {
        chipFilterUser.setOnClickListener { showUserDialog() }
        chipFilterCampus.setOnClickListener { showCampusDialog() }
        chipFilterDate.setOnClickListener { showDateRangeDialog() }

        chipFilterValidated.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chipFilterNotValidated.isChecked = false
                viewModel.filterValidated = true
            } else if (viewModel.filterValidated == true) {
                viewModel.filterValidated = null
            }
            viewModel.applyFilters()
        }

        chipFilterNotValidated.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                chipFilterValidated.isChecked = false
                viewModel.filterValidated = false
            } else if (viewModel.filterValidated == false) {
                viewModel.filterValidated = null
            }
            viewModel.applyFilters()
        }

        chipReset.setOnClickListener {
            viewModel.resetFilters()
            chipFilterUser.text = "Utilisateur";   chipFilterUser.isChecked = false
            chipFilterCampus.text = "Campus";      chipFilterCampus.isChecked = false
            chipFilterDate.text = "Date";          chipFilterDate.isChecked = false
            chipFilterValidated.isChecked    = false
            chipFilterNotValidated.isChecked = false
        }
    }

    // ── Dialog utilisateur avec recherche ─────────────────────────────────────

    private fun showUserDialog() {
        val users = viewModel.allUsers.value ?: return
        showSearchableDialog(
            title          = "Choisir un utilisateur",
            items          = users,
            labelProvider  = { it.name },
            onItemSelected = {
                viewModel.filterUserId   = it.uid
                viewModel.filterUserName = it.name
                chipFilterUser.text = it.name
                chipFilterUser.isChecked = true
                viewModel.applyFilters()
            }
        )
    }

    // ── Dialog campus avec recherche ──────────────────────────────────────────

    private fun showCampusDialog() {
        val campusList = viewModel.campusList.value ?: return

        // Ajouter "Hors campus" en tête de liste
        data class CampusItem(val name: String, val id: String)
        val items = listOf(CampusItem("Hors campus", "HORS_CAMPUS")) +
                campusList.map { CampusItem(it.name, it.name) }

        showSearchableDialog(
            title          = "Choisir un campus",
            items          = items,
            labelProvider  = { it.name },
            onItemSelected = {
                viewModel.filterCampusId = it.id
                chipFilterCampus.text = it.name
                chipFilterCampus.isChecked = true
                viewModel.applyFilters()
            }
        )
    }

    // ── Dialog générique avec champ de recherche ──────────────────────────────

    private fun <T> showSearchableDialog(
        title: String,
        items: List<T>,
        labelProvider: (T) -> String,
        onItemSelected: (T) -> Unit
    ) {
        val dialogView  = layoutInflater.inflate(R.layout.dialog_searchable_list, null)
        val editSearch  = dialogView.findViewById<EditText>(R.id.editSearch)
        val listView    = dialogView.findViewById<ListView>(R.id.listView)

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
                listAdapter = ArrayAdapter(this@PicturesAdminActivity,
                    android.R.layout.simple_list_item_1,
                    filteredList.map { labelProvider(it) })
                listView.adapter = listAdapter
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

    // ── Dialog plage de dates ─────────────────────────────────────────────────

    private fun showDateRangeDialog() {
        val now = Calendar.getInstance()
        DatePickerDialog(this, { _, y1, m1, d1 ->
            val from = Calendar.getInstance().apply { set(y1, m1, d1, 0, 0, 0) }
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val to = Calendar.getInstance().apply { set(y2, m2, d2, 23, 59, 59) }
                viewModel.filterDateFrom = from
                viewModel.filterDateTo   = to
                val fmt = SimpleDateFormat("dd/MM/yy", Locale.FRENCH)
                chipFilterDate.text = "${fmt.format(from.time)} → ${fmt.format(to.time)}"
                chipFilterDate.isChecked = true
                viewModel.applyFilters()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                .apply { setTitle("Sélectionner la date de fin") }.show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            .apply { setTitle("Sélectionner la date de début") }.show()
    }

    // ── Ouvrir le viewer ──────────────────────────────────────────────────────

    private fun openViewer(photo: Map<String, Any>) {
        val loc = photo["location"] as? Map<*, *>
        val state = PhotoViewerState(
            imageUrl          = photo["imageUrl"]          as? String  ?: "",
            family            = photo["family"]            as? String,
            genre             = photo["genre"]             as? String,
            specie            = photo["specie"]            as? String,
            timestamp         = formatTimestamp(photo["timestamp"]),
            adminValidated    = photo["adminValidated"]    as? Boolean ?: false,
            pictureId         = photo["id"]                as? String  ?: "",
            userRef           = photo["userRef"]           as? String  ?: "",
            profilePictureUrl = photo["profilePictureUrl"] as? String,
            censusRef         = photo["censusRef"]         as? String,
            imageBytes        = null,
            latitude          = (loc?.get("latitude")  as? Double) ?: 0.0,
            longitude         = (loc?.get("longitude") as? Double) ?: 0.0,
            altitude          = (loc?.get("altitude")  as? Double) ?: 0.0,
            recordingStatus   = photo["recordingStatus"]   as? Boolean ?: false,
            caller            = PicturesViewerCaller.ADMIN
        )
        PicturesViewerFragment.show(supportFragmentManager, state)
    }

    private fun formatTimestamp(timestamp: Any?): String = when (timestamp) {
        is com.google.firebase.Timestamp ->
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(timestamp.toDate())
        is java.util.Date ->
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(timestamp)
        else -> "Date inconnue"
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class PhotoAdapter(private val items: List<Map<String, Any>>) :
        RecyclerView.Adapter<PhotoAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView   = view.findViewById(R.id.photoItem)
            val recordingDot: View = view.findViewById(R.id.ivDotRed)
            val validatedDot: View = view.findViewById(R.id.ivDotGreen)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val photo          = items[position]
            val url            = photo["imageUrl"] as? String ?: ""
            val adminValidated = photo["adminValidated"]  as? Boolean ?: false
            val recordingStatus= photo["recordingStatus"] as? Boolean ?: false

            Picasso.get().load(url)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .fit().centerCrop()
                .into(holder.image)

            when {
                adminValidated -> {
                    holder.validatedDot.visibility = View.VISIBLE
                    holder.recordingDot.visibility = View.GONE
                }
                !recordingStatus -> {
                    holder.recordingDot.visibility = View.VISIBLE
                    holder.validatedDot.visibility = View.GONE
                }
                else -> {
                    holder.recordingDot.visibility = View.GONE
                    holder.validatedDot.visibility = View.GONE
                }
            }

            holder.image.setOnClickListener { openViewer(photo) }
        }
    }
}