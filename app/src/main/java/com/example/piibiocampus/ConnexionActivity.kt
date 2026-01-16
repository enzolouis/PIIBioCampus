package com.example.piibiocampus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ConnexionActivity : AppCompatActivity() {
    private var pseudoZone: EditText? = null
    private var passwordZone: EditText? = null
    private var connectBtn: Button? = null
    private var createAccountBtn: TextView? = null
    private var username: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connexion)

        pseudoZone = findViewById<EditText?>(R.id.txtIdentifiant)
        passwordZone = findViewById<EditText?>(R.id.txtMdp)
        connectBtn = findViewById<Button?>(R.id.btnConnexion)
        createAccountBtn = findViewById<TextView?>(R.id.btnAlreadyAccount)

        createAccountBtn?.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
        connectBtn?.setOnClickListener {
            // action de se connecter
        }

    }
}