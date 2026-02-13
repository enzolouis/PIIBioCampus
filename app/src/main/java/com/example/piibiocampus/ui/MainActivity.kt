package com.example.piibiocampus.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.piibiocampus.PictureActivity
import com.example.piibiocampus.PreviewPictureActivity
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.map.MapFragment
import com.example.piibiocampus.ui.profiles.MyProfileFragment
import com.example.piibiocampus.utils.DatabaseFiller
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // ton layout précédemment corrigé

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fabCamera = findViewById<FloatingActionButton>(R.id.fabCamera)

        // Charge le fragment par défaut (Actualité si tu en as un)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment()) // par défaut Map pour l'exemple
                .commit()
            bottomNav.selectedItemId = R.id.nav_map
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_actualite -> /* ActualiteFragment() */ MapFragment() // remplace par ton fragment réel
                R.id.nav_recherche -> /* RechercheFragment() */ MapFragment()
                R.id.nav_bibliotheque -> /* BibliothequeFragment() */ MapFragment()
                R.id.nav_map -> MapFragment()
                R.id.nav_compte -> MyProfileFragment()
                else -> null
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
}