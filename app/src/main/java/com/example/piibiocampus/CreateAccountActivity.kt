package com.example.piibiocampus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CreateAccountActivity : AppCompatActivity() {
    private var pseudoZone: EditText? = null
    private var mailZone: EditText? = null
    private var passwordZone: EditText? = null
    private var connectBtn: Button? = null
    private var alreadyAccountBtn: TextView? = null
    private var username: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createaccount)

        pseudoZone = findViewById<EditText?>(R.id.txtIdentifiant)
        mailZone = findViewById<EditText?>(R.id.txtMail)
        passwordZone = findViewById<EditText?>(R.id.txtMdp)
        connectBtn = findViewById<Button?>(R.id.btnConnexion)
        alreadyAccountBtn = findViewById<TextView?>(R.id.btnAlreadyAccount)

        alreadyAccountBtn?.setOnClickListener {
            startActivity(Intent(this, ConnexionActivity::class.java))
        }
        connectBtn?.setOnClickListener {
            // action de créer un compte
        }

    }
}