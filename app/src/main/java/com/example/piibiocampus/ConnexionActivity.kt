package com.example.piibiocampus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ConnexionActivity : AppCompatActivity() {
    private var pseudoZone: EditText? = null
    private var passwordZone: EditText? = null
    private var connectBtn: Button? = null
    private var createAccountBtn: TextView? = null

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

            // Récupérer le texte directement depuis les EditText
            val emailInput = pseudoZone?.text.toString().trim() // renommer pseudoZone en emailZone si tu veux
            val passwordInput = passwordZone?.text.toString().trim()

            // Vérification simple
            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userDao = UserDao()

            lifecycleScope.launch {
                val result = userDao.loginWithEmail(emailInput, passwordInput)

                result
                    .onSuccess { user ->
                        goToMainScreen()
                    }
                    .onFailure { error ->
                        Toast.makeText(
                            this@ConnexionActivity,
                            "Erreur de connexion : ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }

    private fun goToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            goToMainScreen()
        }
    }

}