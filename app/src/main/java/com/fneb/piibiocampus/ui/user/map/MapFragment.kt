package com.fneb.piibiocampus.ui.user.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.model.Campus
import com.fneb.piibiocampus.data.ui.UiState
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.photo.PhotoViewerState
import com.fneb.piibiocampus.ui.photo.PicturesViewerCaller
import com.fneb.piibiocampus.ui.photo.PicturesViewerFragment
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import com.squareup.picasso.Picasso

class MapFragment : Fragment(R.layout.fragment_map) {

    private val viewModel: MapViewModel by viewModels()
    private lateinit var map: MapView

    private val campusTextOverlays = mutableListOf<TextOverlay>()
    private var initialCenterDone  = false

    // ── TextOverlay ───────────────────────────────────────────────────────────

    private inner class TextOverlay(
        private val position: GeoPoint,
        private val text: String
    ) : Overlay() {
        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow) return
            val point = mapView.projection.toPixels(position, null)
            val shadowPaint = Paint().apply {
                color       = Color.BLACK
                textSize    = 36f
                isAntiAlias = true
                typeface    = Typeface.DEFAULT_BOLD
                textAlign   = Paint.Align.CENTER
                maskFilter  = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
            }
            val textPaint = Paint().apply {
                color       = Color.parseColor("#FFB300")
                textSize    = 36f
                isAntiAlias = true
                typeface    = Typeface.DEFAULT_BOLD
                textAlign   = Paint.Align.CENTER
            }
            canvas.drawText(text, point.x.toFloat(), point.y.toFloat(), shadowPaint)
            canvas.drawText(text, point.x.toFloat(), point.y.toFloat(), textPaint)
        }
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireContext().packageName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = view.findViewById(R.id.map)
        setupTileSource()
        observeViewModel()

        viewModel.loadAllPictures()
        viewModel.loadCampus()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        setTopBarTitle(R.string.titleMap)
        InfoWindow.closeAllInfoWindowsOn(map)
        viewModel.loadAllPictures()
        if (!initialCenterDone) centerOnUserLocation()
    }

    override fun onPause()       { super.onPause();   map.onPause() }
    override fun onDestroyView() { super.onDestroyView(); map.overlays.clear() }

    // ── Observation du ViewModel ──────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.picturesState.collect { state ->
                        when (state) {
                            is UiState.Success -> addMarkers(state.data)
                            is UiState.Error   -> showError(state.exception)
                            else               -> Unit
                        }
                    }
                }

                launch {
                    viewModel.campusState.collect { state ->
                        when (state) {
                            is UiState.Success -> addCampusOverlays(state.data)
                            is UiState.Error   -> showError(state.exception)
                            else               -> Unit
                        }
                    }
                }
            }
        }
    }

    // ── Configuration de la carte ─────────────────────────────────────────────

    private fun setupTileSource() {
        val cartoLight = XYTileSource(
            "CartoLight", 0, 20, 256, ".png",
            arrayOf("https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/")
        )
        map.setTileSource(cartoLight)
        map.setTilesScaledToDpi(true)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(
            org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
        )
        map.controller.setZoom(15.0)
        map.minZoomLevel = 3.0
        map.maxZoomLevel = 20.0
        centerOnUserLocation()
    }

    private fun centerOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            map.controller.setCenter(GeoPoint(48.8566, 2.3522))
            return
        }

        LocationServices.getFusedLocationProviderClient(requireActivity())
            .lastLocation
            .addOnSuccessListener { location ->
                val center = if (location != null)
                    GeoPoint(location.latitude, location.longitude)
                else
                    GeoPoint(48.8566, 2.3522)
                map.controller.setCenter(center)
                initialCenterDone = true
            }
    }

    // ── Rendu des marqueurs ───────────────────────────────────────────────────

    private fun addMarkers(points: List<Map<String, Any>>) {
        InfoWindow.closeAllInfoWindowsOn(map)
        map.overlays.removeAll { it is Marker }

        for (o in points) {
            val latLon: Pair<Double, Double> = when {
                o["latitude"] is Double && o["longitude"] is Double ->
                    Pair(o["latitude"] as Double, o["longitude"] as Double)
                o["location"] is Map<*, *> -> {
                    val loc = o["location"] as Map<*, *>
                    val lat = loc["latitude"]  as? Double
                    val lon = loc["longitude"] as? Double
                    if (lat != null && lon != null) Pair(lat, lon) else continue
                }
                else -> continue
            }
            val (lat, lon) = latLon

            val marker = Marker(map).apply {
                position = GeoPoint(lat, lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon  = ContextCompat.getDrawable(requireContext(), R.drawable.pinmap)
                title = (o["specie"] as? String) ?: (o["title"] as? String) ?: ""
            }

            var isOpen = false
            marker.setOnMarkerClickListener { m, _ ->
                if (isOpen) { m.closeInfoWindow(); isOpen = false }
                else        { InfoWindow.closeAllInfoWindowsOn(map); m.showInfoWindow(); isOpen = true }
                true
            }

            marker.infoWindow = object : InfoWindow(R.layout.marker_info, map) {
                override fun onOpen(item: Any?) {
                    val titleView       = mView.findViewById<TextView>(R.id.title)
                    val photoView       = mView.findViewById<ImageView>(R.id.photo)
                    val dotRed: View    = mView.findViewById(R.id.ivDotRed)
                    val dotOrange: View = mView.findViewById(R.id.ivDotOrange)
                    val dotGreen: View  = mView.findViewById(R.id.ivDotGreen)
                    val imageUrl        = (o["imageUrl"] as? String) ?: (o["image"] as? String) ?: ""
                    val adminValidated  = o["adminValidated"]  as? Boolean ?: false
                    val recordingStatus = o["recordingStatus"] as? Boolean ?: false

                    titleView.text = marker.title
                    if (imageUrl.isNotEmpty()) Picasso.get().load(Uri.parse(imageUrl)).into(photoView)

                    dotGreen.visibility  = if (adminValidated)                       View.VISIBLE else View.GONE
                    dotRed.visibility    = if (!adminValidated && !recordingStatus)  View.VISIBLE else View.GONE
                    dotOrange.visibility = if (!adminValidated && recordingStatus)   View.VISIBLE else View.GONE

                    photoView.setOnClickListener {
                        val loc   = o["location"] as? Map<*, *>
                        val state = PhotoViewerState(
                            imageUrl          = imageUrl,
                            family            = o["family"] as? String,
                            genre             = o["genre"]  as? String,
                            specie            = o["specie"] as? String,
                            timestamp         = formatTimestamp(o["timestamp"]),
                            adminValidated    = adminValidated,
                            pictureId         = o["id"]      as? String ?: "",
                            userRef           = o["userRef"] as? String ?: "",
                            profilePictureUrl = o["profilePictureUrl"] as? String,
                            censusRef         = o["censusRef"] as? String,
                            imageBytes        = null,
                            latitude          = (loc?.get("latitude")  as? Double) ?: lat,
                            longitude         = (loc?.get("longitude") as? Double) ?: lon,
                            altitude          = (loc?.get("altitude")  as? Double) ?: 0.0,
                            recordingStatus   = recordingStatus,
                            caller            = PicturesViewerCaller.MAP
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

    // ── Rendu des campus ──────────────────────────────────────────────────────

    private fun addCampusOverlays(campusList: List<Campus>) {
        map.overlays.removeAll { it is Polygon }
        map.overlays.removeAll(campusTextOverlays)
        campusTextOverlays.clear()

        for (campus in campusList) {
            val circle = Polygon(map).apply {
                points = Polygon.pointsAsCircle(
                    GeoPoint(campus.latitudeCenter, campus.longitudeCenter),
                    campus.radius
                )
                fillPaint.color      = Color.argb(40,  255, 179, 0)
                outlinePaint.color   = Color.argb(180, 255, 179, 0)
                outlinePaint.strokeWidth = 3f
                infoWindow = null
                setOnClickListener { _, _, _ -> true }
            }
            map.overlays.add(0, circle)

            val textOverlay = TextOverlay(
                GeoPoint(campus.latitudeCenter, campus.longitudeCenter),
                campus.name
            )
            campusTextOverlays.add(textOverlay)
            val insertIndex = map.overlays.indexOfLast { it is Polygon } + 1
            map.overlays.add(insertIndex, textOverlay)
        }

        val markers = map.overlays.filterIsInstance<Marker>()
        map.overlays.removeAll { it is Marker }
        map.overlays.addAll(markers)

        map.invalidate()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun reloadMarkers() = viewModel.loadAllPictures()

    private fun formatTimestamp(timestamp: Any?): String = when (timestamp) {
        is com.google.firebase.Timestamp ->
            java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.FRENCH)
                .format(timestamp.toDate())
        is java.util.Date ->
            java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.FRENCH)
                .format(timestamp)
        is String -> timestamp
        else      -> "Date inconnue"
    }
}