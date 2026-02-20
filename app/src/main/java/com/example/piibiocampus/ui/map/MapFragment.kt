package com.example.piibiocampus.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.photo.PicturesViewerCaller
import com.example.piibiocampus.ui.photo.PhotoViewerState
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import android.widget.ImageView
import android.widget.TextView
import com.example.piibiocampus.ui.photo.PicturesViewerFragment
import com.squareup.picasso.Picasso

class MapFragment : Fragment(R.layout.fragment_map) {

    private val viewModel: MapViewModel by viewModels()
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = view.findViewById(R.id.map)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTileSource()

        viewModel.pictures.observe(viewLifecycleOwner) { points ->
            addMarkers(points)
        }

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

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                } else {
                    map.controller.setCenter(GeoPoint(43.562817415184526, 1.467314949845769))
                }
            }
        } else {
            map.controller.setCenter(GeoPoint(43.562817415184526, 1.467314949845769))
        }
    }

    private fun addMarkers(points: List<Map<String, Any>>) {
        map.overlays.clear()

        for (o in points) {
            val latLon: Pair<Double, Double>? = when {
                o["latitude"] is Double && o["longitude"] is Double ->
                    Pair(o["latitude"] as Double, o["longitude"] as Double)
                o["location"] is Map<*, *> -> {
                    val loc = o["location"] as Map<*, *>
                    val lat = loc["latitude"] as? Double
                    val lon = loc["longitude"] as? Double
                    if (lat != null && lon != null) Pair(lat, lon) else null
                }
                else -> null
            }
            if (latLon == null) continue

            val marker = Marker(map)
            val (lat, lon) = latLon
            marker.position = GeoPoint(lat, lon)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.pinmap)

            val title = (o["specie"] as? String) ?: (o["title"] as? String) ?: ""
            marker.title = title

            var isOpen = false
            marker.setOnMarkerClickListener { m, _ ->
                if (isOpen) m.closeInfoWindow() else m.showInfoWindow()
                isOpen = !isOpen
                true
            }

            marker.infoWindow = object : InfoWindow(R.layout.marker_info, map) {
                override fun onOpen(item: Any?) {
                    val titleView    = mView.findViewById<TextView>(R.id.title)
                    val photoView    = mView.findViewById<ImageView>(R.id.photo)
                    val badgeView    = mView.findViewById<ImageView>(R.id.ivValidatedBadge)
                    val imageUrl     = (o["imageUrl"] as? String) ?: (o["image"] as? String) ?: ""

                    titleView.text = marker.title
                    if (imageUrl.isNotEmpty()) {
                        Picasso.get().load(Uri.parse(imageUrl)).into(photoView)
                    }

                    // Badge check si adminValidated == true
                    val adminValidated = o["adminValidated"] as? Boolean ?: false
                    badgeView.visibility = if (adminValidated) View.VISIBLE else View.GONE

                    // ── Clic sur la miniature → ouvre PhotoViewerActivity ──
                    photoView.setOnClickListener {
                        val loc = o["location"] as? Map<*, *>
                        val state = PhotoViewerState(
                            imageUrl              = imageUrl,
                            family                = o["family"] as? String,
                            genre                 = o["genre"]  as? String,
                            specie                = o["specie"] as? String,
                            timestamp             = formatTimestamp(o["timestamp"]),
                            adminValidated        = o["adminValidated"] as? Boolean ?: false,
                            pictureId             = o["id"] as? String ?: "",
                            authorId              = o["userId"] as? String ?: "",
                            authorProfilePictureUrl = o["authorProfilePictureUrl"] as? String,
                            censusRef             = o["censusRef"] as? String,
                            imageBytes            = null,   // pas disponible depuis la map
                            latitude              = (loc?.get("latitude") as? Double) ?: lat,
                            longitude             = (loc?.get("longitude") as? Double) ?: lon,
                            altitude              = (loc?.get("altitude") as? Double) ?: 0.0,
                            caller                = PicturesViewerCaller.MAP
                        )
                        PicturesViewerFragment.show(parentFragmentManager, state)
                    }
                }

                override fun onClose() {
                    mView.findViewById<TextView>(R.id.title).text = ""
                    mView.findViewById<ImageView>(R.id.photo).setImageDrawable(null)
                }
            }

            map.overlays.add(marker)
        }

        map.invalidate()
    }

    override fun onResume()      { super.onResume();  map.onResume() }
    override fun onPause()       { super.onPause();   map.onPause() }
    override fun onDestroyView() { super.onDestroyView(); map.overlays.clear() }
}