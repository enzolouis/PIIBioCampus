package com.example.piibiocampus

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    private lateinit var overlay: FrameLayout
    private lateinit var zoomImage: ImageView
    private lateinit var photoDate: TextView
    private lateinit var photoInfos: TextView
    private lateinit var authorButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)

        overlay = findViewById(R.id.overlay)
        zoomImage = findViewById(R.id.zoomImage)
        photoDate = findViewById(R.id.photoDate)
        photoInfos = findViewById(R.id.photoInfos)
        authorButton = findViewById(R.id.authorButton)
        backButton = findViewById(R.id.backButton)

        // click implies back to map
        backButton.setOnClickListener {
            overlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    overlay.visibility = FrameLayout.GONE
                    overlay.alpha = 1f
                }
                .start()
        }



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
        map.controller.setCenter(GeoPoint(43.562817415184526, 1.467314949845769)) // Toulouse by default

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

                            val imageUrl = o["imageUrl"] as String

                            titleView.text = marker.title
                            Picasso.get().load(Uri.parse(imageUrl)).into(photoView)

                            photoView.setOnClickListener {
                                val imageUrl = o["imageUrl"] as String

                                Picasso.get().load(Uri.parse(imageUrl)).into(zoomImage)

                                photoDate.text = "Date : ${o["date"] ?: "Non connu"} à ${o["hours"] ?: "Non connu"}"
                                // joins the table
                                photoInfos.text =
                                    "Genre : ${o["genre"] ?: "Non identifié"}\n" +
                                            "Espèce : ${o["specie"] ?: "Non identifié"}\n" +
                                            "Famille : ${o["family"] ?: "Non identifié"}"

                                authorButton.setOnClickListener {
                                    // link to user pofile (todo)
                                }

                                overlay.apply {
                                    alpha = 0f
                                    visibility = FrameLayout.VISIBLE
                                    animate().alpha(1f).setDuration(200).start()
                                }
                            }
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
