package com.example.piibiocampus

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class MapActivity : AppCompatActivity() {

    private val TAG = "MapActivity"
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- OSMDroid ---
        Configuration.getInstance().userAgentValue = packageName
        map = findViewById(R.id.map)
        val cartoLight = XYTileSource(
            "CartoLight",
            0, 20, 256,
            ".png",
            arrayOf("https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/")
        )
        map.setTileSource(cartoLight)
        map.setTilesScaledToDpi(true)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        map.controller.setCenter(GeoPoint(48.8566, 2.3522)) // Paris

        // --- Coil ImageLoader ---
        val imageLoader = ImageLoader(this)

        // --- Ajouter des markers prédéfinis ---
        val points = listOf(
            Triple("Tour Eiffel", GeoPoint(48.8584, 2.2945), "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Louvre_Museum_Wikimedia.jpg/320px-Louvre_Museum_Wikimedia.jpg"),
            Triple("Louvre", GeoPoint(48.8606, 2.3376), "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Louvre_Museum_Wikimedia.jpg/320px-Louvre_Museum_Wikimedia.jpg")
        )

        for ((title, geoPoint, photoUrl) in points) {
            val marker = Marker(map)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = title

            // InfoWindow custom avec photo
            marker.infoWindow = object : InfoWindow(R.layout.marker_info, map) {
                override fun onOpen(item: Any?) {
                    Log.d(TAG, "InfoWindow opened for marker: $title")
                    val titleView = mView.findViewById<android.widget.TextView>(R.id.title)
                    val photoView = mView.findViewById<android.widget.ImageView>(R.id.photo)
                    titleView.text = marker.title

                    // Coil 1.x
                    val request = ImageRequest.Builder(mView.context)
                        .data(photoUrl)
                        .target(photoView)
                        .crossfade(true)
                        .build()

                    ImageLoader(mView.context).enqueue(request)

                    //super.onOpen(item)
                }

                override fun onClose() {
                    // Rien à faire ici pour l’instant
                    //super.onClose()
                }
            }


            map.overlays.add(marker)
        }

        // --- Firestore : récupérer noms de users ---
        val db = FirebaseFirestore.getInstance()
        db.collection("user")
            .get()
            .addOnSuccessListener { result ->
                val names = result.map { it.getString("name") ?: "Nom inconnu" }
                Log.d(TAG, "Liste des noms : $names")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors de la récupération des users", e)
            }
    }
}
