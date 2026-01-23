package com.example.piibiocampus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {
    private var pseudoZone: EditText? = null
    private var mailZone: EditText? = null
    private var passwordZone: EditText? = null
    private var connectBtn: Button? = null
    private var alreadyAccountBtn: TextView? = null
    private var errorZone: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createaccount)

        pseudoZone = findViewById<EditText?>(R.id.txtIdentifiant)
        mailZone = findViewById<EditText?>(R.id.txtMail)
        passwordZone = findViewById<EditText?>(R.id.txtMdp)
        connectBtn = findViewById<Button?>(R.id.btnConnexion)
        alreadyAccountBtn = findViewById<TextView?>(R.id.btnAlreadyAccount)
        errorZone = findViewById<EditText?>(R.id.errorMsg)

        alreadyAccountBtn?.setOnClickListener {
            startActivity(Intent(this, ConnectionActivity::class.java))
        }
        connectBtn?.setOnClickListener {
            val userName = pseudoZone?.text.toString().trim()
            val email = mailZone?.text.toString().trim()
            val password = passwordZone?.text.toString().trim()

            createAccount(email, password, userName)
        }


    }
    private fun goToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun createAccount(
        email: String,
        password: String,
        userName: String
    ) {

        lifecycleScope.launch {
            UserDao.signUp(email, password, userName)
                .onSuccess {
                    showToast("Compte créé avec succès !")
                    goToMainScreen()
                }
                .onFailure { error ->
                    Log.e("AUTH_error", "error = $error")
                    val userMessage = when (val cause = error.cause?: error) {
                        is FirebaseAuthUserCollisionException ->
                            "Un compte existe déjà avec cet email."
                        is FirebaseException -> {
                            val msg = cause.message ?: ""
                            when {
                                msg.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS") ->
                                    "Mot de passe trop faible : 10 caractères minimum, 1 majuscule, 1 chiffre et 1 caractère spécial"
                                else ->
                                    "Erreur lors de la création du compte."
                            }
                        }
                        else -> "Erreur lors de la création du compte."
                    }
                    errorZone?.text = userMessage
                    //showToast(userMessage, Toast.LENGTH_LONG)
                }
        }
    }


    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }


}