package com.fneb.piibiocampus.ui.admin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.NewsDao
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import com.squareup.picasso.Picasso

class UpdateNewsAdminActivity : BaseActivity() {
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

        val btnValidateUpdate = findViewById<Button>(R.id.btnValideUpdate)
        val btnChangeImage = findViewById<Button>(R.id.btnChangeImage)
        val btnSuppr = findViewById<Button>(R.id.btnSuppr)
        val titleView = findViewById<EditText>(R.id.titleNews)
        imageView = findViewById<ImageView>(R.id.pictureNews)
        val sourceView = findViewById<EditText>(R.id.sourceNews)

        lateinit var idNews: String
        lateinit var imageUrl: String

        val behavior = intent.getStringExtra("behavior")
        val order = intent.getIntExtra("order",0)

        val status = intent.getStringExtra("status")
        if (status == "update"){
            setTopBarTitle("Modifier une Actu'")
            showTopBarLeftButton { finish() }
            btnValidateUpdate.text = "Appliquer"
            btnChangeImage.text = "Changer l'image"

            idNews = intent.getStringExtra("id") ?: ""
            val title = intent.getStringExtra("title")
            imageUrl = intent.getStringExtra("imageUrl")?: ""
            val source = intent.getStringExtra("source")
            titleView.setText(title)
            sourceView.setText(source)

            Picasso.get()
                .load(imageUrl)
                .into(imageView)
        }else{
            setTopBarTitle("Créer une Actu'")
            showTopBarLeftButton { finish() }
            btnValidateUpdate.text = "Créer"
            btnChangeImage.text = "Choisir une image"

            btnSuppr.visibility = View.GONE
            val params = btnValidateUpdate.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = dpToPx(24)
            btnValidateUpdate.layoutParams = params
        }

        btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSuppr.setOnClickListener {
            NewsDao.deleteNews(
                newsId = idNews.toString(),
                onSuccess = {
                    Toast.makeText(this, "Suppression effectuée", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onError = { e ->
                    Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FIREBASE_ERROR", "Erreur lors de la suppression", e)
                }
            )
        }

        btnValidateUpdate.setOnClickListener{
            // si c'est une maj
            if (status == "update"){
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
            // si c'est une création
            }else{
                if (selectedImageUri != null) {
                    NewsDao.uploadNewsImage(
                        context = this,
                        imageUri = selectedImageUri!!,
                        onSuccess = { url ->
                            NewsDao.createNews(
                                titre = findViewById<EditText>(R.id.titleNews).text.toString(),
                                imageUrl = url,
                                source = findViewById<EditText>(R.id.sourceNews).text.toString(),
                                behavior = behavior,
                                order = order,
                                onSuccess = {
                                    Toast.makeText(this, "Actualité créee avec succès", Toast.LENGTH_SHORT).show()
                                    finish()
                                },
                                onError = { e ->
                                    Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                                    Log.e("FIREBASE_ERROR", "Erreur création news", e)
                                }
                            )
                        },
                        onError = { e ->
                            Toast.makeText(
                                this,
                                "Erreur lors de l'upload de l'image : ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }else{
                    Toast.makeText(this, "Veuillez insérez une image et écrire un titre", Toast.LENGTH_LONG).show()
                }
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
                finish()
            },
            onError = { e ->
                Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("FIREBASE_ERROR", "Erreur update", e)
            }
        )
    }
    fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}