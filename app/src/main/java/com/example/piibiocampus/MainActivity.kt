package com.example.piibiocampus

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // Tag pour le log
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Firestore : récupération des noms de users ---
        val db = FirebaseFirestore.getInstance()

        // Supposons que ta collection s'appelle "users"
        db.collection("user")
            .get()
            .addOnSuccessListener { result ->
                // Crée une liste des noms
                val names = result.map { it.getString("name") ?: "Nom inconnu" }
                // Affiche dans le log
                Log.d(TAG, "Liste des noms : $names")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors de la récupération des users", e)
            }
    }
}
