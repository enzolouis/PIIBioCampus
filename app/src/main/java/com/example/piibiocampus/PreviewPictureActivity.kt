package com.example.piibiocampus

import PictureDao
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.databinding.ActivityPreviewPictureBinding
import com.example.piibiocampus.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class PreviewPictureActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityPreviewPictureBinding
    private lateinit var imageBytes: ByteArray

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPreviewPictureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        imageBytes = intent.getByteArrayExtra("imageBytes")!!

        // convertir en Bitmap pour l'affichage
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        viewBinding.imagePreview.setImageBitmap(bitmap)

        viewBinding.btnRetake.setOnClickListener {
            finish()
        }

        viewBinding.btnConfirm.setOnClickListener {
            getCurrentLocation { myCurrentLocation ->
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
    private fun getCurrentLocation(onLocation: (LocationMeta) -> Unit) {

        // si l'acces au GPS refusé
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission GPS refusée", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val meta = LocationMeta(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude
                    )
                    onLocation(meta)
                } else {
                    Toast.makeText(this, "Position indisponible", Toast.LENGTH_SHORT).show()
                }
            }
    }
}