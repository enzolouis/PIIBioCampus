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
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.ImageView
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.MainActivity
import com.example.piibiocampus.ui.admin.DashboardAdminActivity
import com.example.piibiocampus.utils.Extensions.toast
import com.example.piibiocampus.utils.Validators
import kotlinx.coroutines.launch

class ConnectionActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var pseudoZone: EditText
    private lateinit var passwordZone: EditText
    private lateinit var connectBtn: Button
    private lateinit var createAccountBtn: TextView
    private lateinit var resetPassWordBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        pseudoZone = findViewById(R.id.txtIdentifiant)
        passwordZone = findViewById(R.id.txtMdp)
        connectBtn = findViewById(R.id.btnConnexion)
        createAccountBtn = findViewById(R.id.btnAlreadyAccount)
        resetPassWordBtn = findViewById(R.id.btnResetPassWord)

        val togglePassword = findViewById<ImageView>(R.id.btnTogglePassword)

        var isPasswordVisible = false

        togglePassword.setOnClickListener {
            if (isPasswordVisible) {
                passwordZone.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePassword.setImageResource(R.drawable.eye_close)
            } else {
                passwordZone.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePassword.setImageResource(R.drawable.eye_open)
            }

            isPasswordVisible = !isPasswordVisible

            // garder le curseur à la fin
            passwordZone.setSelection(passwordZone.text.length)
        }

        createAccountBtn.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        resetPassWordBtn.setOnClickListener {
            startActivity(Intent(this, ResetPassWordActivity::class.java))
        }

        connectBtn.setOnClickListener {
            val email = pseudoZone.text.toString().trim()
            val password = passwordZone.text.toString().trim()

            if (!Validators.areEmailAndPasswordValid(email, password)) {
                toast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }
        viewModel.checkCurrentUserAndFetchRoleIfNeeded()
        // Observe le state flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {

                        is AuthUiState.Authenticated -> {
                            when (state.role) {
                                "USER" -> startActivity(
                                    Intent(
                                        this@ConnectionActivity,
                                        MainActivity::class.java
                                    )
                                )

                                "ADMIN", "SUPER_ADMIN" -> startActivity(
                                    Intent(
                                        this@ConnectionActivity,
                                        DashboardAdminActivity::class.java
                                    )
                                )

                                else -> toast("Rôle inconnu : ${state.role}")
                            }
                            finish()
                        }

                        is AuthUiState.Error -> {
                            toast(
                                "Erreur de connexion : ${state.throwable.message}",
                                android.widget.Toast.LENGTH_LONG
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}