package com.example.piibiocampus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.piibiocampus.PictureActivity
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.map.MapFragment
import com.example.piibiocampus.ui.profiles.MyProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var fabCamera: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askLocationPermission()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        fabCamera = findViewById(R.id.fabCamera)

        // Charge le fragment par défaut (Map)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment())
                .commit()
            bottomNav.selectedItemId = R.id.nav_map
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_actualite -> MapFragment() // remplace par ton fragment réel
                R.id.nav_recherche -> MapFragment()
                R.id.nav_bibliotheque -> MapFragment()
                R.id.nav_map -> MapFragment()
                R.id.nav_compte -> MyProfileFragment()
                else -> null
            }

            // Gestion de la visibilité du FAB selon l'onglet
            if (item.itemId == R.id.nav_compte) {
                fabCamera.hide()
            } else {
                fabCamera.show()
            }

            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
            }
            true
        }

        fabCamera.setOnClickListener {
            val intent = Intent(this@MainActivity, PictureActivity::class.java)
            startActivity(intent)
        }
    }

    private fun askLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
        } else {
            // fonction qui localise l'utilisateur sur la map
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty()) {
                val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }

                if (granted) {
                    // fonction qui localise l'utilisateur sur la map
                } else {
                    Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}