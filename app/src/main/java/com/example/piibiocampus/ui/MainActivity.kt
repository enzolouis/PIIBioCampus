package com.example.piibiocampus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.piibiocampus.pictures.PictureActivity
import androidx.fragment.app.Fragment
import com.example.piibiocampus.R
import com.example.piibiocampus.news.NewsFragment
import com.example.piibiocampus.ui.map.MapFragment
import com.example.piibiocampus.ui.profiles.MyProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var fabCamera: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    // Instances réutilisées — évite de recréer les fragments à chaque clic
    private val mapFragment     by lazy { MapFragment() }
    private val profileFragment by lazy { MyProfileFragment() }
    private val newsFragment by lazy { NewsFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askLocationPermission()

        bottomNav = findViewById(R.id.bottomNav)
        fabCamera = findViewById(R.id.fabCamera)

        // Charge le fragment par défaut (Map)
        if (savedInstanceState == null) {
            showFragment(mapFragment)
            bottomNav.selectedItemId = R.id.nav_map
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map     -> { fabCamera.show(); showFragment(mapFragment) }
                R.id.nav_compte  -> { fabCamera.hide(); showFragment(profileFragment) }
                // Remplacer par les vrais fragments quand disponibles
                R.id.nav_actualite -> { fabCamera.show(); showFragment(mapFragment) }
                R.id.nav_recherche,
                R.id.nav_bibliotheque -> { fabCamera.show(); showFragment(mapFragment) }
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        fabCamera.setOnClickListener {
            startActivity(Intent(this, PictureActivity::class.java))
        }

        // Gérer le retour depuis CensusTreeActivity uniquement si l'extra est présent.
        // On diffère via post() pour que le fragment soit attaché avant d'appeler reload.
        if (intent?.hasExtra("navigateTo") == true) {
            window.decorView.post { handleNavigationIntent(intent) }
        }
    }

    // Appelé quand MainActivity est déjà en vie (FLAG_ACTIVITY_SINGLE_TOP)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Même protection : on attend que la vue soit prête
        window.decorView.post { handleNavigationIntent(intent) }
    }

    /**
     * Redirige vers le bon onglet et force le rechargement
     * selon l'extra "navigateTo" passé par CensusTreeActivity.
     */
    private fun handleNavigationIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigateTo") ?: return

        when (navigateTo) {
            "myProfile" -> {
                bottomNav.selectedItemId = R.id.nav_compte
                fabCamera.hide()
                showFragment(profileFragment)
                profileFragment.reloadPhotos()
            }
            else -> {
                // Retour sur la map après CREATE
                bottomNav.selectedItemId = R.id.nav_map
                fabCamera.show()
                showFragment(mapFragment)
                mapFragment.reloadMarkers()
            }
        }

        // Consommer l'extra pour ne pas re-déclencher
        intent.removeExtra("navigateTo")
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun askLocationPermission() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (!granted) {
                Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }
}