package com.example.piibiocampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.NewsDao
import com.example.piibiocampus.ui.admin.NewsListAdminActivity.ItemNewsAdapter
import com.example.piibiocampus.utils.setTopBarTitle
import com.squareup.picasso.Picasso

class UpdateNewsAdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private val photos = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ItemNewsAdapter

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
            val imageView = findViewById<ImageView>(R.id.pictureNews)
            val sourceView = findViewById<EditText>(R.id.sourceNews)

            titleView.setText(title)
            sourceView.setText(source)

            Picasso.get()
                .load(imageUrl)
                .into(imageView)
        }

        btnValidateUpdate.setOnClickListener{
            NewsDao.updateNews(
                newsId = idNews.toString(),
                title = findViewById<EditText>(R.id.titleNews).text.toString(),
                source = findViewById<EditText>(R.id.sourceNews).text.toString(),
                imageUrl = imageUrl,
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
}