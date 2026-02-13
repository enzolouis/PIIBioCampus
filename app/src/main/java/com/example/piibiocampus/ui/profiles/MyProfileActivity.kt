package com.example.piibiocampus.ui.profiles

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.UserDao
import androidx.lifecycle.lifecycleScope
import com.example.piibiocampus.databinding.ActivityMyprofileBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class MyProfileActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMyprofileBinding

    private lateinit var recyclerView: RecyclerView

    private val photos = mutableListOf<String>()
    private lateinit var adapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMyprofileBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)



        lifecycleScope.launch {
            val userProfile = UserDao.getCurrentUserProfile()
            val user = UserDao.getCurrentUser()
            if (user != null && userProfile != null) {
                val userId = user.uid
                viewBinding.pseudoText.text = userProfile.name
                viewBinding.description.text = userProfile.description
                Picasso.get().load(Uri.parse(userProfile.profilePictureUrl)).into(viewBinding.profilePicture)

                PictureDao.getPicturesByUser(userId,
                    onSuccess = { pictures ->
                        photos.clear()

                        for (picture in pictures) {
                            val url = picture["imageUrl"] as? String
                            if (url != null) {
                                photos.add(url)
                            }
                        }

                        adapter.notifyDataSetChanged()
                    },
                    onError = { exception ->
                        exception.printStackTrace()
                    },
                )
            }
        }

        recyclerView = findViewById(R.id.photosRecycler)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = PhotoAdapter(photos)
        recyclerView.adapter = adapter
    }

    inner class PhotoAdapter(private val photos: List<String>) :
        RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.photoItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val url = photos[position]

            Picasso.get()
                .load(url)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .fit()
                .centerCrop()
                .into(holder.image)

        }

        override fun getItemCount(): Int = photos.size
    }
}
