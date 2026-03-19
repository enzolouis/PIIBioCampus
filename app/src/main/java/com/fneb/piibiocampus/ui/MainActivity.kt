package com.fneb.piibiocampus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.fneb.piibiocampus.pictures.PictureActivity
import androidx.fragment.app.Fragment
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.news.NewsFragment
import com.fneb.piibiocampus.ui.library.LibraryFragment
import com.fneb.piibiocampus.ui.map.MapFragment
import com.fneb.piibiocampus.ui.profiles.MyProfileFragment
import com.fneb.piibiocampus.ui.searchUsers.SearchUsersFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var fabCamera: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    private val mapFragment     by lazy { MapFragment() }
    private val profileFragment by lazy { MyProfileFragment() }
    private val newsFragment    by lazy { NewsFragment() }
    private val libraryFragment by lazy { LibraryFragment() }
    private val searchUsersFragment by lazy { SearchUsersFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askLocationPermission()

        bottomNav = findViewById(R.id.bottomNav)
        fabCamera = findViewById(R.id.fabCamera)
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)

        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime        = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            if (imeVisible) {
                bottomNav.visibility = View.GONE
                fabCamera.hide()
                view.setPadding(0, 0, 0, ime.bottom)
            } else {
                bottomNav.visibility = View.VISIBLE
                if (bottomNav.selectedItemId == R.id.nav_map) fabCamera.show()
                // Padding = hauteur réelle de la bottomNav (inclut déjà les system bars via fitsSystemWindows)
                view.post {
                    view.setPadding(0, 0, 0, bottomNav.height)
                }
            }
            insets
        }

        if (savedInstanceState == null) {
            showFragment(mapFragment)
            bottomNav.selectedItemId = R.id.nav_map
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map          -> { fabCamera.show(); showFragment(mapFragment);         true }
                R.id.nav_compte       -> { fabCamera.hide(); showFragment(profileFragment);     true }
                R.id.nav_actualite    -> { fabCamera.hide(); showFragment(newsFragment);        true }
                R.id.nav_bibliotheque -> { fabCamera.hide(); showFragment(libraryFragment);     true }
                R.id.nav_recherche    -> { fabCamera.hide(); showFragment(searchUsersFragment); true }
                else -> false
            }
            true
        }

        fabCamera.setOnClickListener {
            startActivity(Intent(this, PictureActivity::class.java))
        }

        handleNavigationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent) {
        val target = intent.getStringExtra("navigateTo") ?: return
        when (target) {
            "myProfile" -> {
                bottomNav.selectedItemId = R.id.nav_compte
                fabCamera.hide()
                showFragment(profileFragment)
            }
            "map" -> {
                bottomNav.selectedItemId = R.id.nav_map
                fabCamera.show()
                showFragment(mapFragment)
                window.decorView.post { mapFragment.reloadMarkers() }
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

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