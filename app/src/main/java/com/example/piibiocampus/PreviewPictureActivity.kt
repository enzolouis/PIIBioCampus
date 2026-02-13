package com.example.piibiocampus

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.piibiocampus.data.model.LocationMeta
import com.example.piibiocampus.databinding.ActivityPreviewPictureBinding
import com.example.piibiocampus.ui.census.CensusTreeActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class PreviewPictureActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityPreviewPictureBinding
    private lateinit var imageBytes: ByteArray
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_TIMEOUT_MS = 10000L // 10 secondes
    private var locationHandler: Handler? = null
    private var isLocationFetched = false

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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

    private fun fetchLocationAndContinue() {
        // Vérifier la permission à nouveau (sécurité)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission GPS refusée", Toast.LENGTH_SHORT).show()
            return
        }

        isLocationFetched = false

        // Afficher un message de chargement
        Toast.makeText(this, "Récupération de la position...", Toast.LENGTH_SHORT).show()

        // Timeout handler
        locationHandler = Handler(Looper.getMainLooper())
        locationHandler?.postDelayed({
            if (!isLocationFetched) {
                Toast.makeText(
                    this,
                    "Impossible d'obtenir la position GPS. Utilisation de la dernière position connue.",
                    Toast.LENGTH_LONG
                ).show()
                // Fallback sur lastLocation
                getLastKnownLocation()
            }
        }, LOCATION_TIMEOUT_MS)

        // Essayer d'obtenir la position actuelle avec priorité haute
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (!isLocationFetched && location != null) {
                isLocationFetched = true
                locationHandler?.removeCallbacksAndMessages(null)
                proceedToCensus(location.latitude, location.longitude, location.altitude)
            } else if (!isLocationFetched) {
                // Si null, essayer lastLocation
                getLastKnownLocation()
            }
        }.addOnFailureListener {
            if (!isLocationFetched) {
                getLastKnownLocation()
            }
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission GPS requise", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (!isLocationFetched) {
                    isLocationFetched = true
                    locationHandler?.removeCallbacksAndMessages(null)

                    if (location != null) {
                        proceedToCensus(location.latitude, location.longitude, location.altitude)
                    } else {
                        Toast.makeText(
                            this,
                            "Position GPS indisponible. Vérifiez que le GPS est activé.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                if (!isLocationFetched) {
                    isLocationFetched = true
                    locationHandler?.removeCallbacksAndMessages(null)
                    Toast.makeText(
                        this,
                        "Erreur GPS: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun proceedToCensus(latitude: Double, longitude: Double, altitude: Double) {
        val intent = Intent(this, CensusTreeActivity::class.java)
        intent.putExtra("imageBytes", imageBytes)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        intent.putExtra("altitude", altitude)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHandler?.removeCallbacksAndMessages(null)
    }
}