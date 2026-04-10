package com.fneb.piibiocampus.ui.user.profiles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.MainActivity
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.ui.user.profiles.settings.SettingsFragment
import com.fneb.piibiocampus.utils.BadgeUtils
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class MyProfileFragment : Fragment() {

    private lateinit var recyclerView:    RecyclerView
    private lateinit var pseudoText:      TextView
    private lateinit var description:     TextView
    private lateinit var profilePicture:  ImageView
    private lateinit var badge:           ImageView
    private lateinit var settingsButton:  ImageView
    private var progressBar: View? = null

    private val viewModel: ProfileViewModel by viewModels { MyProfileViewModelFactory() }

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_myprofile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTopBarTitle(R.string.titleProfile)

        recyclerView   = view.findViewById(R.id.photosRecycler)
        pseudoText     = view.findViewById(R.id.pseudoText)
        description    = view.findViewById(R.id.description)
        profilePicture = view.findViewById(R.id.profilePicture)
        badge          = view.findViewById(R.id.badge)
        settingsButton = view.findViewById(R.id.settingsButton)
        progressBar    = view.findViewById(R.id.progressBar)

        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter
        recyclerView.clipToPadding = false

        setupButtons()
        setupObservers()

        // Recharge les photos au retour du PicturesViewerFragment
        parentFragmentManager.setFragmentResultListener(
            PicturesViewerFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ -> viewModel.reloadPhotos() }
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleProfile)
        photos.clear()
        adapter.notifyDataSetChanged()
        viewModel.reloadPhotos()
        setupObservers()
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    private fun setupButtons() {
        settingsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
            }
        )
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    bindProfile(state.data)
                }
                is UiState.Error -> {
                    showLoading(false)
                    showError(state.exception)
                }
                else -> Unit
            }
        }

        viewModel.photosState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> { /* spinner géré par profileState */ }
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

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindProfile(profile: UserProfile) {
        pseudoText.text  = profile.name
        description.text = profile.description

        if (profile.profilePictureUrl.isNotEmpty()) {
            Picasso.get()
                .load(Uri.parse(profile.profilePictureUrl))
                .placeholder(R.drawable.photo_placeholder)
                .into(profilePicture)
        } else {
            profilePicture.setImageResource(R.drawable.ic_profile)
        }

        val badgeRes = if (profile.currentBadge.isNotEmpty()) {
            BadgeUtils.getDrawableById(profile.currentBadge)
        } else {
            R.drawable.norank
        }
        badge.setImageResource(badgeRes)
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

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

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photo    = items[position]
            val imageUrl = photo["imageUrl"] as? String ?: ""

            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .fit()
                .centerCrop()
                .into(holder.image)

            val recordingStatus = photo["recordingStatus"] as? Boolean ?: false
            val adminValidated  = photo["adminValidated"]  as? Boolean ?: false

            when {
                adminValidated -> {
                    holder.validatedDot.visibility       = View.VISIBLE
                    holder.recordingDotRed.visibility    = View.GONE
                    holder.recordingDotOrange.visibility = View.GONE
                }
                !recordingStatus -> {
                    holder.recordingDotRed.visibility    = View.VISIBLE
                    holder.validatedDot.visibility       = View.GONE
                    holder.recordingDotOrange.visibility = View.GONE
                }
                else -> {
                    holder.recordingDotRed.visibility    = View.GONE
                    holder.validatedDot.visibility       = View.GONE
                    holder.recordingDotOrange.visibility = View.VISIBLE
                }
            }

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
                caller            = PicturesViewerCaller.MY_PROFILE,
                recordingStatus   = photo["recordingStatus"] as? Boolean ?: false
            )
            PicturesViewerFragment.show(parentFragmentManager, state)
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