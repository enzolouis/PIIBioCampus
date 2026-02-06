package com.example.piibiocampus

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.LifecycleCameraController
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.databinding.ActivityPictureBinding
import com.example.piibiocampus.databinding.ActivityPreviewPictureBinding
import com.google.firebase.auth.FirebaseAuth

class PreviewPictureActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPreviewPictureBinding
    private lateinit var imageBytes: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPreviewPictureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        imageBytes = intent.getByteArrayExtra("imageBytes")!!
        // Convertir en Bitmap pour l'affichage
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
                    finish()
                },
                onError = { e ->
                    Log.e("UPLOAD", "Erreur upload", e)
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }

            )
        }
    }
}