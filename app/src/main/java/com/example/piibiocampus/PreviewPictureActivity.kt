package com.example.piibiocampus

import PictureDao
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.databinding.ActivityPreviewPictureBinding
import com.example.piibiocampus.ui.MainActivity

class PreviewPictureActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPreviewPictureBinding
    private lateinit var imageBytes: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPreviewPictureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        imageBytes = intent.getByteArrayExtra("imageBytes")!!

        // convertir en Bitmap pour l'affichage
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        viewBinding.imagePreview.setImageBitmap(bitmap)

        viewBinding.btnRetake.setOnClickListener {
            finish()
        }

        viewBinding.btnConfirm.setOnClickListener {
            val myCurrentLocation = LocationMeta(0.0, 0.0, 0.0) // placeholder
            PictureDao.exportPictureFromBytes(
                context = this,
                imageBytes = imageBytes,
                location = myCurrentLocation,
                censusRef = "",
                userRef = null,
                speciesRef = "",
                onSuccess = {
                    Toast.makeText(this, "Photo correcement envoyé", Toast.LENGTH_SHORT).show()

                    // emmener vers le recensement à l'avenir
                    val intent = Intent(this@PreviewPictureActivity, MainActivity::class.java)
                    startActivity(intent)
                },
                onError = { e ->
                    Log.e("UPLOAD", "Erreur lors de l'envoi de la photo", e)
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }

            )
        }
    }
}