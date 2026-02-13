package com.example.piibiocampus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.databinding.ActivityPreviewPictureBinding
import com.example.piibiocampus.ui.census.CensusTreeActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class PreviewPictureActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityPreviewPictureBinding
    private lateinit var imageBytes: ByteArray

    // 🔹 Permission launcher moderne
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchLocationAndContinue()
            } else {
                Toast.makeText(
                    this,
                    "La localisation est obligatoire pour le recensement",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPreviewPictureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        imageBytes = intent.getByteArrayExtra("imageBytes")!!

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        viewBinding.imagePreview.setImageBitmap(bitmap)

        viewBinding.btnRetake.setOnClickListener {
            finish()
        }

        viewBinding.btnConfirm.setOnClickListener {
            checkPermissionAndProceed()
        }
    }

    // 🔹 Vérifie permission
    private fun checkPermissionAndProceed() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {

                fetchLocationAndContinue()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // 🔹 Récupère position puis lance CensusTreeActivity
    private fun fetchLocationAndContinue() {

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {

                        val meta = LocationMeta(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude
                        )

                        val intent = Intent(
                            this@PreviewPictureActivity,
                            CensusTreeActivity::class.java
                        )

                        intent.putExtra("imageBytes", imageBytes)
                        intent.putExtra("latitude", meta.latitude)
                        intent.putExtra("longitude", meta.longitude)
                        intent.putExtra("altitude", meta.altitude)

                        startActivity(intent)
                    }
                },
                null
            )

        } catch (e: SecurityException) {
            Toast.makeText(this, "Erreur permission localisation", Toast.LENGTH_LONG).show()
        }
    }
}
