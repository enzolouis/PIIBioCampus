package com.example.piibiocampus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {
    private var pseudoZone: EditText? = null
    private var mailZone: EditText? = null
    private var passwordZone: EditText? = null
    private var connectBtn: Button? = null
    private var alreadyAccountBtn: TextView? = null

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
            val userNameInput = pseudoZone?.text.toString().trim()
            val emailInput = mailZone?.text.toString().trim()
            val passwordInput = passwordZone?.text.toString().trim()

            if (userNameInput.isEmpty() || emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordStrong(passwordInput)) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial !", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userDao = UserDao()

            lifecycleScope.launch {
                val result = userDao.signup(emailInput, passwordInput, userNameInput)

                result
                    .onSuccess { user ->
                        Toast.makeText(this@CreateAccountActivity, "Compte créé avec succès !", Toast.LENGTH_SHORT).show()
                        goToMainScreen()
                    }
                    .onFailure { error ->
                        Toast.makeText(
                            this@CreateAccountActivity,
                            "Erreur lors de la création du compte : ${error.message}",
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

    private fun isPasswordStrong(password: String): Boolean {
        if (password.length < 12) return false

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { "!@#\$%^&*()_+-=[]{}|;:'\",.<>?/".contains(it) }

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }


}