package com.fneb.piibiocampus.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.fneb.piibiocampus.ui.user.pictures.PictureActivity
import androidx.fragment.app.Fragment
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.ui.user.news.NewsFragment
import com.fneb.piibiocampus.ui.user.library.LibraryFragment
import com.fneb.piibiocampus.ui.user.map.MapFragment
import com.fneb.piibiocampus.ui.user.profiles.MyProfileFragment
import com.fneb.piibiocampus.ui.user.searchUsers.SearchUsersFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : BaseActivity() {

    private lateinit var fabCamera: FloatingActionButton
    private lateinit var bottomNav: BottomNavigationView

    private val mapFragment         by lazy { MapFragment() }
    private val profileFragment     by lazy { MyProfileFragment() }
    private val newsFragment        by lazy { NewsFragment() }
    private val libraryFragment     by lazy { LibraryFragment() }
    private val searchUsersFragment by lazy { SearchUsersFragment() }

    private var activeFragment: Fragment = mapFragment

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
                view.post {
                    view.setPadding(0, 0, 0, bottomNav.height)
                }
            }
            insets
        }

        if (savedInstanceState == null) {
            // Ajoute tous les fragments une seule fois, tous cachés sauf la map
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, mapFragment,         "map")
                add(R.id.fragment_container, profileFragment,     "profile")
                add(R.id.fragment_container, newsFragment,        "news")
                add(R.id.fragment_container, libraryFragment,     "library")
                add(R.id.fragment_container, searchUsersFragment, "search")
                hide(profileFragment)
                hide(newsFragment)
                hide(libraryFragment)
                hide(searchUsersFragment)
            }.commit()

            activeFragment = mapFragment
            bottomNav.selectedItemId = R.id.nav_map
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStackImmediate(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }

            // 2. Changer d'onglet normalement
            when (item.itemId) {
                R.id.nav_map          -> { fabCamera.show(); showFragment(mapFragment);         true }
                R.id.nav_compte       -> { fabCamera.hide(); showFragment(profileFragment);     true }
                R.id.nav_actualite    -> { fabCamera.hide(); showFragment(newsFragment);        true }
                R.id.nav_bibliotheque -> { fabCamera.hide(); showFragment(libraryFragment);     true }
                R.id.nav_recherche    -> { fabCamera.hide(); showFragment(searchUsersFragment); true }
                else -> false
            }
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

    // show/hide au lieu de replace — les fragments ne sont jamais détruits
    private fun showFragment(fragment: Fragment) {
        if (fragment == activeFragment) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
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