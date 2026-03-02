package com.example.piibiocampus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.piibiocampus.PictureActivity
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.map.MapFragment
import com.example.piibiocampus.ui.profiles.MyProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var fabCamera: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    // Instances réutilisées — évite de recréer les fragments à chaque navigation
    private val mapFragment     by lazy { MapFragment() }
    private val profileFragment by lazy { MyProfileFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askLocationPermission()

        bottomNav = findViewById(R.id.bottomNav)
        fabCamera = findViewById(R.id.fabCamera)

        if (savedInstanceState == null) {
            showFragment(mapFragment)
            bottomNav.selectedItemId = R.id.nav_map
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map    -> { fabCamera.show(); showFragment(mapFragment);     true }
                R.id.nav_compte -> { fabCamera.hide(); showFragment(profileFragment); true }
                R.id.nav_actualite,
                R.id.nav_recherche,
                R.id.nav_bibliotheque -> { fabCamera.show(); showFragment(mapFragment); true }
                else -> false
            }
        }

        fabCamera.setOnClickListener {
            startActivity(Intent(this, PictureActivity::class.java))
        }

        // Gérer un éventuel navigateTo passé à la création
        // (cas où MainActivity n'existait pas encore dans la pile)
        handleNavigationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // FLAG_ACTIVITY_SINGLE_TOP → l'instance existante reçoit onNewIntent
        // C'est le cas normal après CREATE depuis CensusTreeActivity
        handleNavigationIntent(intent)
    }

    // ── Navigation + reload ───────────────────────────────────────────────────

    private fun handleNavigationIntent(intent: Intent) {
        val target = intent.getStringExtra("navigateTo") ?: return

        when (target) {
            "myProfile" -> {
                bottomNav.selectedItemId = R.id.nav_compte
                fabCamera.hide()
                showFragment(profileFragment)
                // Le listener Firestore de MyProfileFragment se relance dans onResume()
                // → pas besoin d'appel explicite
            }
            "map" -> {
                bottomNav.selectedItemId = R.id.nav_map
                fabCamera.show()
                showFragment(mapFragment)
                // Recharger les marqueurs après ajout d'une nouvelle photo
                // Différé d'un frame pour s'assurer que le fragment est attaché
                window.decorView.post {
                    mapFragment.reloadMarkers()
                }
            }
        }
    }

    // ── Fragments ─────────────────────────────────────────────────────────────

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun askLocationPermission() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
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
        if (requestCode == 100 && grantResults.none { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_SHORT).show()
        }
    }
}