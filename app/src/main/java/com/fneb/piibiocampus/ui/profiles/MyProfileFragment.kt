package com.fneb.piibiocampus.ui.profiles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.ui.MainActivity
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MyProfileFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pseudoText: TextView
    private lateinit var description: TextView
    private lateinit var profilePicture: ImageView
    private lateinit var badge: ImageView

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    private var firestoreListener: ListenerRegistration? = null
    private var currentUserId: String? = null

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

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter
        recyclerView.clipToPadding = false

        // Bouton retour système → retour vers MainActivity
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
            }
        )

        // Résultat du viewer (suppression, mise à jour de recensement)
        parentFragmentManager.setFragmentResultListener(
            PicturesViewerFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            val uid = currentUserId ?: return@setFragmentResultListener
            setupPhotosListener(uid)
        }

        loadUserDataAndListenToPhotos()
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private fun loadUserDataAndListenToPhotos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userProfile = UserDao.getCurrentUserProfile()
            val user        = UserDao.getCurrentUser()

            if (user != null && userProfile != null) {
                currentUserId = user.uid

                pseudoText.text  = userProfile.name
                description.text = userProfile.description

                if (!userProfile.profilePictureUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(Uri.parse(userProfile.profilePictureUrl))
                        .placeholder(R.drawable.photo_placeholder)
                        .into(profilePicture)
                }

                setupPhotosListener(user.uid)
            }
        }
    }

    private fun setupPhotosListener(userId: String) {
        firestoreListener?.remove()
        firestoreListener = PictureDao.listenToPicturesByUserEnrichedSortedByDate(userId) { enrichedPhotos ->
            photos.clear()
            photos.addAll(enrichedPhotos)

            val badgeRes = when {
                enrichedPhotos.size >= 100 -> R.drawable.ic_badge_cerf_erudit
                enrichedPhotos.size >= 90  -> R.drawable.ic_badge_chouette_savante
                enrichedPhotos.size >= 80  -> R.drawable.ic_badge_renard_ruse
                enrichedPhotos.size >= 70  -> R.drawable.ic_badge_sanglier_chercheur
                enrichedPhotos.size >= 60  -> R.drawable.ic_badge_pie_futee
                enrichedPhotos.size >= 50  -> R.drawable.ic_badge_ecureuil_eclaire
                enrichedPhotos.size >= 40  -> R.drawable.ic_badge_blaireau_fouineur
                enrichedPhotos.size >= 30  -> R.drawable.ic_badge_herisson_debrouillard
                enrichedPhotos.size >= 20  -> R.drawable.ic_badge_lapin_malin
                enrichedPhotos.size >= 10  -> R.drawable.ic_badge_scarabe_astucieux
                enrichedPhotos.size >= 1   -> R.drawable.ic_badge_abeille_curieuse
                else                       -> R.drawable.norank
            }
            badge.setImageResource(badgeRes)
            adapter.notifyDataSetChanged()
        }
    }

    fun reloadPhotos() {
        val uid = currentUserId ?: return
        setupPhotosListener(uid)
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleProfile)
        val uid = currentUserId ?: return
        setupPhotosListener(uid)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class PhotoAdapter(private val items: List<Map<String, Any>>) :
        RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView   = view.findViewById(R.id.photoItem)
            val recordingDotRed: View = view.findViewById(R.id.ivDotRed)
            val recordingDotOrange: View = view.findViewById(R.id.ivDotOrange)
            val validatedDot: View = view.findViewById(R.id.ivDotGreen)
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
                    holder.recordingDotRed.visibility = View.GONE
                    holder.recordingDotOrange.visibility = View.GONE
                }
                !recordingStatus -> {
                    holder.recordingDotRed.visibility = View.VISIBLE
                    holder.validatedDot.visibility = View.GONE
                    holder.recordingDotOrange.visibility = View.GONE
                }
                else -> {
                    holder.recordingDotRed.visibility = View.GONE
                    holder.validatedDot.visibility = View.GONE
                    holder.recordingDotOrange.visibility = View.VISIBLE
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
                imageUrl          = photo["imageUrl"] as? String ?: "",
                family            = photo["family"]   as? String,
                genre             = photo["genre"]    as? String,
                specie            = photo["specie"]   as? String,
                timestamp         = formatTimestamp(photo["timestamp"]),
                adminValidated    = photo["adminValidated"]  as? Boolean ?: false,
                pictureId         = photo["id"]              as? String  ?: "",
                userRef           = currentUserId            ?: "",
                profilePictureUrl = null,
                censusRef         = photo["censusRef"]       as? String,
                imageBytes        = null,
                latitude          = (loc?.get("latitude")  as? Double) ?: 0.0,
                longitude         = (loc?.get("longitude") as? Double) ?: 0.0,
                altitude          = (loc?.get("altitude")  as? Double) ?: 0.0,
                caller            = PicturesViewerCaller.MY_PROFILE,
                recordingStatus   = photo["recordingStatus"] as? Boolean ?: false
            )
            PicturesViewerFragment.show(parentFragmentManager, state)
        }
    }
}