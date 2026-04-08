package com.fneb.piibiocampus.ui.profiles

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.UserProfile
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class UserProfileFragment : Fragment() {

    private var recyclerView:   RecyclerView? = null
    private var pseudoText:     TextView?     = null
    private var description:    TextView?     = null
    private var profilePicture: ImageView?    = null
    private var badge:          ImageView?    = null
    private var progressBar:    View?         = null

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    private val viewModel: ProfileViewModel by viewModels {
        val userId = arguments?.getString(ARG_USER_ID)
            ?: error("UserProfileFragment requiert un userId")
        UserProfileViewModelFactory(userId)
    }

    companion object {
        private const val ARG_USER_ID = "userId"

        fun newInstance(userId: String) = UserProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_USER_ID, userId) }
        }
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTopBarTitle(R.string.titleProfile)

        // Vues initialisées AVANT setupObservers pour éviter le crash lateinit
        recyclerView   = view.findViewById(R.id.photosRecycler)
        pseudoText     = view.findViewById(R.id.pseudoText)
        description    = view.findViewById(R.id.description)
        profilePicture = view.findViewById(R.id.profilePicture)
        badge          = view.findViewById(R.id.badge)
        progressBar    = view.findViewById(R.id.progressBar)

        adapter = PhotoAdapter(photos)
        recyclerView?.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView?.adapter = adapter

        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView   = null
        pseudoText     = null
        description    = null
        profilePicture = null
        badge          = null
        progressBar    = null
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
                    updateBadge(state.data.size)
                    adapter.notifyDataSetChanged()
                }
                is UiState.Error -> showError(state.exception)
                else -> Unit
            }
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindProfile(profile: UserProfile) {
        pseudoText?.text  = profile.name
        description?.text = profile.description

        if (!profile.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(Uri.parse(profile.profilePictureUrl))
                .placeholder(R.drawable.photo_placeholder)
                .into(profilePicture)
        } else {
            profilePicture?.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun updateBadge(count: Int) {
        val badgeRes = when {
            count >= 100 -> R.drawable.ic_badge_cerf_erudit
            count >= 90  -> R.drawable.ic_badge_chouette_savante
            count >= 80  -> R.drawable.ic_badge_renard_ruse
            count >= 70  -> R.drawable.ic_badge_sanglier_chercheur
            count >= 60  -> R.drawable.ic_badge_pie_futee
            count >= 50  -> R.drawable.ic_badge_ecureuil_eclaire
            count >= 40  -> R.drawable.ic_badge_blaireau_fouineur
            count >= 30  -> R.drawable.ic_badge_herisson_debrouillard
            count >= 20  -> R.drawable.ic_badge_lapin_malin
            count >= 10  -> R.drawable.ic_badge_scarabe_astucieux
            count >= 1   -> R.drawable.ic_badge_abeille_curieuse
            else         -> R.drawable.norank
        }
        badge?.setImageResource(badgeRes)
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

            holder.validatedDot.visibility       = if (adminValidated)                       View.VISIBLE else View.GONE
            holder.recordingDotRed.visibility    = if (!adminValidated && !recordingStatus)  View.VISIBLE else View.GONE
            holder.recordingDotOrange.visibility = if (!adminValidated && recordingStatus)   View.VISIBLE else View.GONE

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