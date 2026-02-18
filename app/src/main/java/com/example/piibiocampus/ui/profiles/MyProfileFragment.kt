package com.example.piibiocampus.ui.profiles

import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.UserDao
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.Locale

class MyProfileFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pseudoText: TextView
    private lateinit var description: TextView
    private lateinit var profilePicture: ImageView
    private lateinit var badge: ImageView


    private lateinit var overlay: FrameLayout
    private lateinit var zoomImage: ImageView
    private lateinit var photoDate: TextView
    private lateinit var photoInfos: TextView
    private lateinit var backButton: Button

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: PhotoAdapter

    // pour couper l'écoute Firestore quand on quitte le fragment
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_myprofile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.photosRecycler)
        pseudoText = view.findViewById(R.id.pseudoText)
        description = view.findViewById(R.id.description)
        profilePicture = view.findViewById(R.id.profilePicture)
        badge = view.findViewById(R.id.badge)

        overlay = view.findViewById(R.id.overlay)
        zoomImage = view.findViewById(R.id.zoomImage)
        photoDate = view.findViewById(R.id.photoDate)
        photoInfos = view.findViewById(R.id.photoInfos)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter

        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            overlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    overlay.visibility = FrameLayout.GONE
                    overlay.alpha = 1f
                }
                .start()
        }

        loadUserDataAndListenToPhotos()
    }

    private fun loadUserDataAndListenToPhotos() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userProfile = UserDao.getCurrentUserProfile()
            val user = UserDao.getCurrentUser()

            if (user != null && userProfile != null) {
                val userId = user.uid

                // init profil (not update)
                pseudoText.text = userProfile.name
                description.text = userProfile.description


                if (!userProfile.profilePictureUrl.isNullOrEmpty()) {
                    Picasso.get()
                        .load(Uri.parse(userProfile.profilePictureUrl))
                        .placeholder(R.drawable.photo_placeholder)
                        .into(profilePicture)
                }

                // updates photos in real time
                setupPhotosListener(userId)
            }
        }
    }

    private fun setupPhotosListener(userId: String) {
        // on annule l'ancien listener s'il existe déjà
        firestoreListener?.remove()

        // Écoute en temps réel avec tri par date (plus récent en premier)
        firestoreListener = PictureDao.listenToPicturesByUserEnrichedSortedByDate(userId) { enrichedPhotos ->
            photos.clear()
            photos.addAll(enrichedPhotos)

            val badgeRes = when {
                enrichedPhotos.size >= 50 -> R.drawable.diamond
                enrichedPhotos.size >= 20 -> R.drawable.gold
                enrichedPhotos.size >= 10 -> R.drawable.silver
                enrichedPhotos.size >= 2 -> R.drawable.bronze
                else -> R.drawable.norank
            }
            badge.setImageResource(badgeRes)

            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
    inner class PhotoAdapter(private val photos: List<Map<String, Any>>) :
        RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.photoItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo, parent, false)
            return PhotoViewHolder(view)
        }

        private fun formatTimestamp(timestamp: Any?): String {
            return when (timestamp) {
                is com.google.firebase.Timestamp -> {
                    val date = timestamp.toDate()
                    val format = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH)
                    format.format(date)
                }
                is java.util.Date -> {
                    val format = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH)
                    format.format(timestamp)
                }
                is String -> timestamp
                else -> "Date inconnue"
            }
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photo = photos[position]
            val imageUrl = photo["imageUrl"] as? String ?: ""

            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .fit()
                .centerCrop()
                .into(holder.image)

            holder.image.setOnClickListener {
                // Zoom image
                if (imageUrl.isNotEmpty()) {
                    Picasso.get().load(imageUrl).into(zoomImage)
                } else {
                    zoomImage.setImageDrawable(null)
                }

                val timestamp = formatTimestamp(photo["timestamp"])
                val family = photo["family"] ?: "Non identifié"
                val genre = photo["genre"] ?: "Non identifié"
                val specie = photo["specie"] ?: "Non identifié"

                photoDate.text = "Date : $timestamp"

                // Affichage adapté selon le niveau taxonomique
                photoInfos.text = "Famille : $family\nGenre : $genre\nEspèce : $specie"



                overlay.apply {
                    alpha = 0f
                    visibility = FrameLayout.VISIBLE
                    animate().alpha(1f).setDuration(200).start()
                }
            }
        }

        override fun getItemCount(): Int = photos.size
    }

}