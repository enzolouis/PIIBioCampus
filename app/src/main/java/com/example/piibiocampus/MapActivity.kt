package com.example.piibiocampus

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
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

        // configuration open streeet map
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

        // coil image loader
        val imageLoader = ImageLoader(this)

        // ajouter des markers prédéfinis placeholder
        val points2 = listOf(
            Pair("Tour Eiffel", GeoPoint(48.8584, 2.2945)),
            Pair("Louvre", GeoPoint(48.8606, 2.3376))
        )

        var points: List<Map<String,Any>>? = null

        PictureDao.getAllPictures(
            onSuccess = { points ->

                Log.d("test", "$points")

                for (o in points) {
                    val marker = Marker(map)
                    val lat = o["latitude"] as Double
                    val lon = o["longitude"] as Double

                    marker.position = GeoPoint(lat, lon)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    marker.icon = ContextCompat.getDrawable(this, R.drawable.pinmap)

                    var isOpen = false
                    marker.setOnMarkerClickListener { m, mapView ->
                        if (isOpen) {
                            m.closeInfoWindow()
                        } else {
                            m.showInfoWindow()
                        }
                        isOpen = !isOpen
                        true
                    }

                    marker.infoWindow = object : InfoWindow(R.layout.marker_info, map) {
                        override fun onOpen(item: Any?) {
                            val titleView = mView.findViewById<TextView>(R.id.title)
                            val photoView = mView.findViewById<ImageView>(R.id.photo)

                            titleView.text = marker.title

                            Picasso.get().load(Uri.parse(o["imageUrl"] as String)).into(photoView)
                        }

                        override fun onClose() {
                            val titleView = mView.findViewById<TextView>(R.id.title)
                            val photoView = mView.findViewById<ImageView>(R.id.photo)

                            titleView.text = ""
                            photoView.setImageDrawable(null)
                        }
                    }

                    map.overlays.add(marker)
                }

                map.invalidate() // redraw map
            },
            onError = { e ->
                Log.e(TAG, "Erreur récupération de toutes les images", e)
            }
        )

    }
}
