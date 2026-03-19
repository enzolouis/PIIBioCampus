package com.fneb.piibiocampus.ui.profiles

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class UserProfileFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pseudoText: TextView
    private lateinit var description: TextView
    private lateinit var profilePicture: ImageView
    private lateinit var badge: ImageView

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    companion object {
        private const val ARG_USER_ID = "userId"

        fun newInstance(userId: String) = UserProfileFragment().apply {
            arguments = Bundle().apply { putString(ARG_USER_ID, userId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString(ARG_USER_ID) ?: return

        setTopBarTitle(R.string.titleProfile)

        recyclerView   = view.findViewById(R.id.photosRecycler)
        pseudoText     = view.findViewById(R.id.pseudoText)
        description    = view.findViewById(R.id.description)
        profilePicture = view.findViewById(R.id.profilePicture)
        badge          = view.findViewById(R.id.badge)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = PhotoAdapter(photos, userId)
        recyclerView.adapter = adapter

        loadUserProfile(userId)
    }

    private fun loadUserProfile(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val userProfile = UserDao.getUserProfileById(userId)
            val photoList   = PictureDao.getPicturesByUserEnrichedSortedByDate(userId)

            if (userProfile != null) {
                pseudoText.text  = userProfile.name
                description.text = userProfile.description

                if (!userProfile.profilePictureUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(Uri.parse(userProfile.profilePictureUrl))
                        .placeholder(R.drawable.photo_placeholder)
                        .into(profilePicture)
                }
            }

            photos.clear()
            photos.addAll(photoList)
            updateBadge(photoList.size)
            adapter.notifyDataSetChanged()
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
        badge.setImageResource(badgeRes)
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class PhotoAdapter(
        private val items: List<Map<String, Any>>,
        private val userId: String
    ) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView   = view.findViewById(R.id.photoItem)
            val recordingDot: View = view.findViewById(R.id.ivRecordingDot)
            val validatedDot: View = view.findViewById(R.id.ivValidatedBadge)
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

            holder.image.setOnClickListener { openPhotoViewer(photo) }
        }

        private fun formatTimestamp(timestamp: Any?): String = when (timestamp) {
            is com.google.firebase.Timestamp ->
                SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(timestamp.toDate())
            is java.util.Date ->
                SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH).format(timestamp)
            is String -> timestamp
            else      -> "Date inconnue"
        }

        private fun openPhotoViewer(photo: Map<String, Any>) {
            val loc = photo["location"] as? Map<*, *>
            val state = PhotoViewerState(
                imageUrl          = photo["imageUrl"]        as? String ?: "",
                family            = photo["family"]          as? String,
                genre             = photo["genre"]           as? String,
                specie            = photo["specie"]          as? String,
                timestamp         = formatTimestamp(photo["timestamp"]),
                adminValidated    = photo["adminValidated"]  as? Boolean ?: false,
                pictureId         = photo["id"]              as? String  ?: "",
                userRef           = userId,
                profilePictureUrl = null,
                censusRef         = photo["censusRef"]       as? String,
                imageBytes        = null,
                latitude          = (loc?.get("latitude")   as? Double) ?: 0.0,
                longitude         = (loc?.get("longitude")  as? Double) ?: 0.0,
                altitude          = (loc?.get("altitude")   as? Double) ?: 0.0,
                caller            = PicturesViewerCaller.MAP,
                recordingStatus   = photo["recordingStatus"] as? Boolean ?: false
            )
            PicturesViewerFragment.show(parentFragmentManager, state)
        }
    }
}