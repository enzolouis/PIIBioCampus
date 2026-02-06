package com.example.piibiocampus.ui.auth

import ResetPasswordViewModel
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.piibiocampus.R
import com.example.piibiocampus.utils.Extensions.toast
import kotlinx.coroutines.launch

class ResetPassWordActivity : AppCompatActivity() {

    private val viewModel: ResetPasswordViewModel by viewModels()

    private lateinit var pseudoZone: EditText
    private lateinit var sendBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        pseudoZone = findViewById(R.id.txtIdentifiant)
        sendBtn = findViewById(R.id.btnReinitialiserMotDePasse)

        sendBtn.setOnClickListener {
            val email = pseudoZone.text.toString().trim()
            if (email.isEmpty()) {
                toast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }
            viewModel.sendResetEmail(email)
        }

        // Observer le flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ResetPasswordUiState.Loading -> sendBtn.isEnabled = false
                        ResetPasswordUiState.Idle -> sendBtn.isEnabled = true
                        ResetPasswordUiState.EmailSent -> {
                            sendBtn.isEnabled = true
                            toast("Email de réinitialisation envoyé")
                        }
                        is ResetPasswordUiState.Error -> {
                            sendBtn.isEnabled = true
                            toast(state.message)
                        }
                    }
                }
            }
        }
    }
}
