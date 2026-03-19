package com.example.piibiocampus.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.NewsDao
import com.example.piibiocampus.ui.admin.NewsListAdminActivity.ItemNewsAdapter
import com.example.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso

class UpdateNewsAdminActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_news_admin)
        setTopBarTitle("Modification Actu'")

        val status = intent.getIntExtra("status", -1)
        //Log.d("test",status.toString())

        val btnValidateUpdate = findViewById<Button>(R.id.btnValideUpdate)

        lateinit var idNews: String
        lateinit var imageUrl: String
        if (status == 0){
            idNews = intent.getStringExtra("id") ?: ""
            val title = intent.getStringExtra("title")
            imageUrl = intent.getStringExtra("imageUrl")?: ""
            val source = intent.getStringExtra("source")

            val titleView = findViewById<EditText>(R.id.titleNews)
            imageView = findViewById<ImageView>(R.id.pictureNews)
            val sourceView = findViewById<EditText>(R.id.sourceNews)

            titleView.setText(title)
            sourceView.setText(source)

            Picasso.get()
                .load(imageUrl)
                .into(imageView)
        }

        val btnChangeImage = findViewById<Button>(R.id.btnChangeImage)

        btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnValidateUpdate.setOnClickListener{
            // si changement d'image
            if (selectedImageUri != null) {
                NewsDao.uploadNewsImage(
                    context = this,
                    imageUri = selectedImageUri!!,
                    onSuccess = { url ->
                        updateInfos(idNews.toString(), url)
                    },
                    onError = { e ->
                        Toast.makeText(this, "Erreur lors de l'upload de l'image : ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            // pas de changement d'image
            }else{
                updateInfos(idNews.toString(), imageUrl)
            }
        }
    }

    fun updateInfos(newsId : String, url : String){
        NewsDao.updateNews(
            newsId = newsId.toString(),
            title = findViewById<EditText>(R.id.titleNews).text.toString(),
            source = findViewById<EditText>(R.id.sourceNews).text.toString(),
            imageUrl = url,
            onSuccess = {
                Toast.makeText(this, "Mise à jour effectuée", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, NewsListAdminActivity::class.java)
                this.startActivity(intent)
            },
            onError = { e ->
                Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("FIREBASE_ERROR", "Erreur update", e)
            }
        )
    }
}