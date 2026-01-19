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
                    showToast(
                        "Erreur lors de la création du compte : ${error.message}",
                        Toast.LENGTH_LONG
                    )
                }
        }
    }


    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }


}