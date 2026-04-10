package com.fneb.piibiocampus.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.ui.common.LoadingDialog
import com.fneb.piibiocampus.utils.Extensions.toast
import com.fneb.piibiocampus.utils.Validators
import kotlinx.coroutines.launch

class CreateAccountActivity : BaseActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createaccount)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnConnexion)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (20 * resources.displayMetrics.density).toInt()
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                systemBars.bottom + extraPadding
            insets
        }

        val username      = findViewById<EditText>(R.id.txtIdentifiant)
        val email         = findViewById<EditText>(R.id.txtMail)
        val password      = findViewById<EditText>(R.id.txtMdp)
        val connectionBtn = findViewById<Button>(R.id.btnAlreadyAccount)
        val errorZone     = findViewById<TextView>(R.id.errorMsg)

        connectionBtn.setOnClickListener {
            startActivity(Intent(this, ConnectionActivity::class.java))
            finish()
        }

        val togglePassword    = findViewById<ImageView>(R.id.btnTogglePassword)
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
                LoadingDialog.show(supportFragmentManager, "Création du compte…")
                viewModel.register(emailStr, passwordStr, usernameStr)
            } else {
                CguDialogFragment.show(
                    fm         = supportFragmentManager,
                    onAccepted = {
                        LoadingDialog.show(supportFragmentManager, "Création du compte…")
                        viewModel.register(emailStr, passwordStr, usernameStr)
                    },
                    onDeclined = { toast("Vous devez accepter les CGU pour créer un compte") }
                )
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> Unit

                    is AuthUiState.EmailVerificationSent -> {
                        LoadingDialog.hide(supportFragmentManager)
                        EmailSentDialogFragment.show(
                            fm        = supportFragmentManager,
                            mode      = EmailSentDialogFragment.MODE_ACCOUNT_CREATED,
                            onDismiss = {
                                startActivity(
                                    Intent(this@CreateAccountActivity, ConnectionActivity::class.java)
                                        .apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                )
                                finish()
                            }
                        )
                    }

                    is AuthUiState.Error -> {
                        LoadingDialog.hide(supportFragmentManager)
                        errorZone.text = state.exception.userMessage
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun isCguAccepted(): Boolean =
        getSharedPreferences(CguDialogFragment.PREF_FILE, MODE_PRIVATE)
            .getBoolean(CguDialogFragment.PREF_KEY, false)
}