package com.example.piibiocampus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ConnectionActivity : AppCompatActivity() {
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
            val email = pseudoZone?.text.toString().trim()
            val password = passwordZone?.text.toString().trim()

            if (!areInputsValid(email, password)) return@setOnClickListener

            login(email, password)
        }

    }

    private fun goToMainScreen() {
        val userDao = UserDao()
        val currentUser = userDao.getCurrentUser()

        if (currentUser == null) {
            showToast("Utilisateur non connecté")
            return
        }

        lifecycleScope.launch {
            userDao.getRoleByUid(currentUser.uid)
                .onSuccess { role ->
                    when (role) {
                        "USER" -> {
                            startActivity(
                                Intent(this@ConnectionActivity, MainActivity::class.java)
                            )
                        }

                        "ADMIN", "SUPER_ADMIN" -> {
                            startActivity(
                                Intent(this@ConnectionActivity, DashboardAdminActivity::class.java)
                            )
                        }

                        else -> {
                            showToast("Rôle inconnu : $role")
                            return@onSuccess
                        }
                    }
                    finish()
                }
                .onFailure { error ->
                    showToast(
                        "Erreur lors de la récupération du rôle : ${error.message}",
                        Toast.LENGTH_LONG
                    )
                }
        }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            goToMainScreen()
        }
    }


    private fun areInputsValid(
        email: String,
        password: String
    ): Boolean {
        return if (email.isEmpty() || password.isEmpty()) {
            showToast("Veuillez remplir tous les champs")
            false
        } else {
            true
        }
    }


    private fun login(
        email: String,
        password: String
    ) {
        val userDao = UserDao()

        lifecycleScope.launch {
            userDao.loginWithEmail(email, password)
                .onSuccess {
                    goToMainScreen()
                }
                .onFailure { error ->
                    showToast(
                        "Erreur de connexion : ${error.message}",
                        Toast.LENGTH_LONG
                    )
                }
        }
    }


    private fun showToast(
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(this, message, duration).show()
    }


}