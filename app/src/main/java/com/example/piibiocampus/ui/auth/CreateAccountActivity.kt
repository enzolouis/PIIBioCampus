package com.example.piibiocampus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.MainActivity
import com.example.piibiocampus.utils.Extensions.toast
import com.example.piibiocampus.utils.Validators
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createaccount)

        val username      = findViewById<EditText>(R.id.txtIdentifiant)
        val email         = findViewById<EditText>(R.id.txtMail)
        val password      = findViewById<EditText>(R.id.txtMdp)
        val connectionBtn = findViewById<Button>(R.id.btnAlreadyAccount)
        val errorZone     = findViewById<TextView>(R.id.errorMsg)

        connectionBtn.setOnClickListener {
            startActivity(Intent(this, ConnectionActivity::class.java))
        }

        val togglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        var isPasswordVisible = false

        togglePassword.setOnClickListener {
            if (isPasswordVisible) {
                password.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePassword.setImageResource(R.drawable.eye_close)
            } else {
                password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePassword.setImageResource(R.drawable.eye_open)
            }
            isPasswordVisible = !isPasswordVisible
            password.setSelection(password.text.length)
        }

        findViewById<Button>(R.id.btnConnexion).setOnClickListener {
            val emailStr    = email.text.toString().trim()
            val passwordStr = password.text.toString().trim()
            val usernameStr = username.text.toString().trim()

            if (!Validators.areEmailAndPasswordValid(emailStr, passwordStr)) {
                toast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            if (isCguAccepted()) {
                viewModel.register(emailStr, passwordStr, usernameStr)
            } else {
                CguDialogFragment.show(
                    fm         = supportFragmentManager,
                    onAccepted = { viewModel.register(emailStr, passwordStr, usernameStr) },
                    onDeclined = { toast("Vous devez accepter les CGU pour créer un compte") }
                )
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> { /* afficher un loader si besoin */ }

                    is AuthUiState.Registered -> {
                        toast("Compte créé avec succès !")
                        startActivity(Intent(this@CreateAccountActivity, MainActivity::class.java))
                        finish()
                    }

                    is AuthUiState.Error -> {
                        errorZone.text =
                            state.throwable.message ?: "Erreur lors de la création du compte"
                    }

                    else -> Unit
                }
            }
        }
    }

    // ── CGU ───────────────────────────────────────────────────────────────────

    private fun isCguAccepted(): Boolean =
        getSharedPreferences(CguDialogFragment.PREF_FILE, MODE_PRIVATE)
            .getBoolean(CguDialogFragment.PREF_KEY, false)
}