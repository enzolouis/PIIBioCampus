package com.fneb.piibiocampus.ui.admin.searchUsersAdmin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.ui.user.profiles.ProfileViewModel
import com.fneb.piibiocampus.ui.user.profiles.UserProfileViewModelFactory
import com.fneb.piibiocampus.utils.BadgeUtils
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class UserProfileAdminActivity : BaseActivity() {

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
    }

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    private lateinit var recyclerView:   RecyclerView
    private lateinit var pseudoText:     TextView
    private lateinit var description:    TextView
    private lateinit var profilePicture: ImageView
    private lateinit var badge:          ImageView
    private var progressBar: View? = null

    private val viewModel: ProfileViewModel by viewModels {
        val userId = intent.getStringExtra(EXTRA_USER_ID)
            ?: error("UserProfileAdminActivity requiert un userId")
        UserProfileViewModelFactory(userId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_user_profile)

        if (intent.getStringExtra(EXTRA_USER_ID) == null) { finish(); return }

        setTopBarTitle("Profile")
        showTopBarLeftButton { finish() }

        recyclerView   = findViewById(R.id.photosRecycler)
        pseudoText     = findViewById(R.id.pseudoText)
        description    = findViewById(R.id.description)
        profilePicture = findViewById(R.id.profilePicture)
        badge          = findViewById(R.id.badge)
        progressBar    = findViewById(R.id.progressBar)

        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.profileState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> { showLoading(false); bindProfile(state.data) }
                is UiState.Error   -> { showLoading(false); showError(state.exception) }
                else -> Unit
            }
        }

        viewModel.photosState.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    photos.clear()
                    photos.addAll(state.data)
                    adapter.notifyDataSetChanged()
                }
                is UiState.Error -> showError(state.exception)
                else -> Unit
            }
        }
    }

    private fun bindProfile(profile: UserProfile) {
        pseudoText.text  = profile.name
        description.text = profile.description

        if (!profile.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(Uri.parse(profile.profilePictureUrl))
                .placeholder(R.drawable.photo_placeholder)
                .into(profilePicture)
        } else {
            profilePicture.setImageResource(R.drawable.ic_profile)
        }

        badge.setImageResource(
            if (profile.currentBadge.isNotEmpty())
                BadgeUtils.getDrawableById(profile.currentBadge)
            else
                R.drawable.norank
        )
    }

    private fun showLoading(visible: Boolean) {
        progressBar?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class PhotoAdapter(private val items: List<Map<String, Any>>) :
        RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image:              ImageView = view.findViewById(R.id.photoItem)
            val recordingDotRed:    View      = view.findViewById(R.id.ivDotRed)
            val recordingDotOrange: View      = view.findViewById(R.id.ivDotOrange)
            val validatedDot:       View      = view.findViewById(R.id.ivDotGreen)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo, parent, false)
            return PhotoViewHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photo       = items[position]
            val imageUrl    = photo["imageUrl"]       as? String  ?: ""
            val adminVal    = photo["adminValidated"] as? Boolean ?: false
            val recStatus   = photo["recordingStatus"] as? Boolean ?: false

            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .fit().centerCrop()
                .into(holder.image)

            holder.validatedDot.visibility       = if (adminVal)                  View.VISIBLE else View.GONE
            holder.recordingDotRed.visibility    = if (!adminVal && !recStatus)   View.VISIBLE else View.GONE
            holder.recordingDotOrange.visibility = if (!adminVal && recStatus)    View.VISIBLE else View.GONE

            holder.image.setOnClickListener { openPhotoViewer(photo) }
        }

        private fun openPhotoViewer(photo: Map<String, Any>) {
            val loc = photo["location"] as? Map<*, *>
            val state = PhotoViewerState(
                imageUrl          = photo["imageUrl"]        as? String  ?: "",
                family            = photo["family"]          as? String,
                genre             = photo["genre"]           as? String,
                specie            = photo["specie"]          as? String,
                timestamp         = formatTimestamp(photo["timestamp"]),
                adminValidated    = photo["adminValidated"]  as? Boolean ?: false,
                pictureId         = photo["id"]              as? String  ?: "",
                userRef           = photo["userRef"]         as? String  ?: "",
                profilePictureUrl = null,
                censusRef         = photo["censusRef"]       as? String,
                imageBytes        = null,
                latitude          = (loc?.get("latitude")   as? Double) ?: 0.0,
                longitude         = (loc?.get("longitude")  as? Double) ?: 0.0,
                altitude          = (loc?.get("altitude")   as? Double) ?: 0.0,
                recordingStatus   = photo["recordingStatus"] as? Boolean ?: false,
                caller            = PicturesViewerCaller.USER_PROFILE
            )
            PicturesViewerFragment.show(supportFragmentManager, state)
        }

        private fun formatTimestamp(timestamp: Any?): String = when (timestamp) {
            is com.google.firebase.Timestamp ->
                SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(timestamp.toDate())
            is java.util.Date ->
                SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(timestamp)
            is String -> timestamp
            else      -> "Date inconnue"
        }
    }
}