package com.example.piibiocampus.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.piibiocampus.R
import com.squareup.picasso.Picasso
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import androidx.fragment.app.viewModels
import androidx.core.net.toUri
import com.google.android.gms.location.LocationServices


class MapFragment : Fragment(R.layout.fragment_map) {

    private val TAG = "MapFragment"
    private val viewModel: MapViewModel by viewModels()

    private lateinit var map: MapView
    private lateinit var overlay: FrameLayout
    private lateinit var zoomImage: ImageView
    private lateinit var photoDate: TextView
    private lateinit var photoInfos: TextView
    private lateinit var authorButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        overlay = view.findViewById(R.id.overlay)
        zoomImage = view.findViewById(R.id.zoomImage)
        photoDate = view.findViewById(R.id.photoDate)
        photoInfos = view.findViewById(R.id.photoInfos)
        authorButton = view.findViewById(R.id.authorButton)
        backButton = view.findViewById(R.id.backButton)
        map = view.findViewById(R.id.map)
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

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTileSource()

        // Observe le ViewModel
        viewModel.pictures.observe(viewLifecycleOwner) { points ->
            addMarkers(points)
        }

        // Charge les points (par défaut tous)
        viewModel.loadAllPictures()
    }

    private fun formatTimestamp(timestamp: Any?): String {
        return when (timestamp) {
            is com.google.firebase.Timestamp -> {
                val date = timestamp.toDate()
                val format = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.FRENCH)
                format.format(date)
            }
            is java.util.Date -> {
                val format = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.FRENCH)
                format.format(timestamp)
            }
            is String -> timestamp
            else -> "Date inconnue"
        }
    }

    private fun setupTileSource() {
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

        // Centrer sur la position actuelle de l'utilisateur (sans marker)
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                } else {
                    // Fallback sur Toulouse si pas de position
                    map.controller.setCenter(GeoPoint(43.562817415184526, 1.467314949845769))
                }
            }
        } else {
            // Fallback sur Toulouse si pas de permission
            map.controller.setCenter(GeoPoint(43.562817415184526, 1.467314949845769))
        }
    }

    private fun addMarkers(points: List<Map<String, Any>>) {
        map.overlays.clear()

        for (o in points) {
            val latLon: Pair<Double, Double>? = when {
                o["latitude"] is Double && o["longitude"] is Double -> {
                    Pair(o["latitude"] as Double, o["longitude"] as Double)
                }
                o["location"] is Map<*, *> -> {
                    val loc = o["location"] as Map<*, *>
                    val lat = (loc["latitude"] as? Double)
                    val lon = (loc["longitude"] as? Double)
                    if (lat != null && lon != null) Pair(lat, lon) else null
                }
                else -> null
            }

            if (latLon == null) continue

            val marker = Marker(map)
            val (lat, lon) = latLon
            marker.position = GeoPoint(lat, lon)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // icône du marker (pinmap) — assure-toi d'avoir R.drawable.pinmap
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.pinmap)

            // titre de marker (si présent)
            val title = (o["specie"] as? String) ?: (o["title"] as? String) ?: ""
            marker.title = title

            var isOpen = false
            marker.setOnMarkerClickListener { m, mapView ->
                if (isOpen) m.closeInfoWindow() else m.showInfoWindow()
                isOpen = !isOpen
                true
            }

            marker.infoWindow = object : InfoWindow(R.layout.marker_info, map) {
                override fun onOpen(item: Any?) {
                    val titleView = mView.findViewById<TextView>(R.id.title)
                    val photoView = mView.findViewById<ImageView>(R.id.photo)

                    val imageUrl = when {
                        o["imageUrl"] is String -> o["imageUrl"] as String
                        (o["image"] is String) -> o["image"] as String
                        else -> null
                    }

                    titleView.text = marker.title
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(Uri.parse(imageUrl)).into(photoView)
                    } else {
                        photoView.setImageDrawable(null)
                    }

                    photoView.setOnClickListener {
                        if (!imageUrl.isNullOrEmpty()) {
                            Picasso.get().load(imageUrl.toUri()).into(zoomImage)
                        } else {
                            zoomImage.setImageDrawable(null)
                        }

                        photoDate.text = "Date : ${formatTimestamp(o["timestamp"])}"
                        photoInfos.text =
                            "Famille : ${o["family"] ?: "Non identifié"}\n" +
                                    "Genre : ${o["genre"] ?: "Non identifié"}\n" +
                                    "Espèce : ${o["specie"] ?: "Non identifié"}"


                        authorButton.setOnClickListener {
                            // todo: naviguer vers le profil de l'auteur si tu as userRef
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

        map.invalidate()
    }

    // Lifecycle important pour MapView (OSMDroid)
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // sécurité : clear overlays
        map.overlays.clear()
    }
}
