package com.fneb.piibiocampus.ui.auth

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.utils.Extensions.toast
import kotlinx.coroutines.launch

class ResetPassWordActivity : BaseActivity() {

    private val viewModel: ResetPasswordViewModel by viewModels()

    private lateinit var pseudoZone: EditText
    private lateinit var sendBtn:    Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resetpassword)

        pseudoZone = findViewById(R.id.txtIdentifiant)
        sendBtn    = findViewById(R.id.btnReinitialiserMotDePasse)

        ViewCompat.setOnApplyWindowInsetsListener(sendBtn) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (20 * resources.displayMetrics.density).toInt()
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                systemBars.bottom + extraPadding
            insets
        }

        sendBtn.setOnClickListener {
            val email = pseudoZone.text.toString().trim()
            if (email.isEmpty()) { toast("Veuillez remplir tous les champs"); return@setOnClickListener }
            viewModel.sendResetEmail(email)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ResetPasswordUiState.Loading  -> sendBtn.isEnabled = false
                        ResetPasswordUiState.Idle     -> sendBtn.isEnabled = true
                        ResetPasswordUiState.EmailSent -> {
                            sendBtn.isEnabled = true
                            toast("Email de réinitialisation envoyé")
                        }
                        is ResetPasswordUiState.Error -> {
                            sendBtn.isEnabled = true
                            showError(state.exception)
                        }
                        is ResetPasswordUiState.CooldownError -> {
                            sendBtn.isEnabled = true
                            toast("Veuillez attendre ${state.secondsLeft} secondes avant une nouvelle demande")
                        }
                    }
                }
            }
        }
    }
}