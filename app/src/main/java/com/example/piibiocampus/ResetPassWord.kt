package com.example.piibiocampus

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


class ResetPassWord : AppCompatActivity() {
    private var pseudoZone: EditText? = null

    private var sendBtn: Button? = null

    private var lastResetRequestTime = 0L
    private val RESET_COOLDOWN = 60_000L // 60 secondes


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        pseudoZone = findViewById<EditText?>(R.id.txtIdentifiant)

        sendBtn = findViewById<Button?>(R.id.btnReinitialiserMotDePasse)

        sendBtn?.setOnClickListener {

            val email = pseudoZone?.text.toString().trim()

            if (!areInputValid(email)) return@setOnClickListener

            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastResetRequestTime

            if (elapsedTime < RESET_COOLDOWN) {
                val secondsLeft = ((RESET_COOLDOWN - elapsedTime) / 1000)
                showToast("Veuillez attendre $secondsLeft secondes avant une nouvelle demande")
                return@setOnClickListener
            }

            sendBtn?.isEnabled = false
            lastResetRequestTime = currentTime
            sendEmail(email)
        }



        sendBtn?.postDelayed({
            sendBtn?.isEnabled = true
        }, RESET_COOLDOWN)
    }

    private fun areInputValid(
        email: String
    ): Boolean {
        return if (email.isEmpty()) {
            showToast("Veuillez remplir tous les champs")
            false
        } else {
            true
        }
    }

    private fun showToast(
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(this, message, duration).show()
    }


    private fun ResetPassWord.sendEmail(email: String) {
        val auth = FirebaseAuth.getInstance()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Email de réinitialisation envoyé")
                } else {
                    showToast("Erreur : ${task.exception?.message}")
                }
            }
    }


}


