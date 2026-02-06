package com.example.piibiocampus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.map.MapActivity
import com.example.piibiocampus.utils.Extensions.toast
import com.example.piibiocampus.utils.Validators
import kotlinx.coroutines.launch

class CreateAccountActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createaccount)

        val username = findViewById<EditText>(R.id.txtIdentifiant)
        val email = findViewById<EditText>(R.id.txtMail)
        val password = findViewById<EditText>(R.id.txtMdp)
        val errorZone = findViewById<TextView>(R.id.errorMsg)

        findViewById<Button>(R.id.btnConnexion).setOnClickListener {
            if (!Validators.areEmailAndPasswordValid(
                    email.text.toString().trim(),
                    password.text.toString().trim()
                )
            ) {
                toast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            viewModel.register(
                email.text.toString().trim(),
                password.text.toString().trim(),
                username.text.toString().trim()
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            // show loader if needed
                        }

                        is AuthUiState.Registered -> {
                            toast("Compte créé avec succès !")
                            startActivity(
                                Intent(
                                    this@CreateAccountActivity,
                                    MapActivity::class.java
                                )
                            )
                            finish()
                        }

                        is AuthUiState.Error -> {
                            // ici, tu peux mapper l'exception à des messages plus friendly
                            errorZone.text =
                                state.throwable.message ?: "Erreur lors de la création du compte"
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}