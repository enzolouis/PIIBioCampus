    package com.fneb.piibiocampus.ui.auth

    import android.content.Intent
    import android.os.Bundle
    import android.view.View
    import android.view.ViewGroup
    import android.text.method.HideReturnsTransformationMethod
    import android.text.method.PasswordTransformationMethod
    import android.widget.Button
    import android.widget.EditText
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.activity.viewModels
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.lifecycle.lifecycleScope
    import com.fneb.piibiocampus.R
    import com.fneb.piibiocampus.politiqueDeConfidentialite.PrivacyPolicyDialogFragment
    import com.fneb.piibiocampus.ui.MainActivity
    import com.fneb.piibiocampus.ui.admin.DashboardAdminActivity
    import com.fneb.piibiocampus.ui.common.LoadingDialog
    import com.fneb.piibiocampus.utils.Extensions.toast
    import com.fneb.piibiocampus.utils.Validators
    import kotlinx.coroutines.launch

    class ConnectionActivity : AppCompatActivity() {

        companion object {
            /** Passé par CreateAccountActivity — non utilisé ici, la pop-up remplace le bandeau */
            const val EXTRA_VERIFICATION_SENT = "extra_verification_sent"
        }

        private val viewModel: AuthViewModel by viewModels()

        private lateinit var pseudoZone:       EditText
        private lateinit var passwordZone:     EditText
        private lateinit var connectBtn:       Button
        private lateinit var createAccountBtn: Button
        private lateinit var resetPassWordBtn: Button

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_connection)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnConnexion)) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val extraPadding = (20 * resources.displayMetrics.density).toInt()
                (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBars.bottom + extraPadding
                insets
            }

            pseudoZone       = findViewById(R.id.txtIdentifiant)
            passwordZone     = findViewById(R.id.txtMdp)
            connectBtn       = findViewById(R.id.btnConnexion)
            createAccountBtn = findViewById(R.id.btnNoAccount)
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
                passwordZone.setSelection(passwordZone.text.length)
            }

            createAccountBtn.setOnClickListener {
                startActivity(Intent(this, CreateAccountActivity::class.java))
            }

            resetPassWordBtn.setOnClickListener {
                startActivity(Intent(this, ResetPassWordActivity::class.java))
            }

            connectBtn.setOnClickListener {
                val email    = pseudoZone.text.toString().trim()
                val password = passwordZone.text.toString().trim()

                if (!Validators.areEmailAndPasswordValid(email, password)) {
                    toast("Veuillez remplir tous les champs")
                    return@setOnClickListener
                }

                if (isCguAccepted()) {
                    if (isPrivacyPolicyAccepted()) {
                        LoadingDialog.show(supportFragmentManager, "Connexion en cours…")
                        viewModel.login(email, password)
                    } else {
                        showPrivacyPolicyDialog(
                            onAccepted = {
                                LoadingDialog.show(supportFragmentManager, "Connexion en cours…")
                                viewModel.login(email, password)
                            }
                        )
                    }
                } else {
                    showCguDialog(
                        onAccepted = {
                            if (isPrivacyPolicyAccepted()) {
                                LoadingDialog.show(supportFragmentManager, "Connexion en cours…")
                                viewModel.login(email, password)
                            } else {
                                showPrivacyPolicyDialog(
                                    onAccepted = {
                                        LoadingDialog.show(supportFragmentManager, "Connexion en cours…")
                                        viewModel.login(email, password)
                                    }
                                )
                            }
                        }
                    )
                }
            }

            viewModel.checkCurrentUserAndFetchRoleIfNeeded()

            lifecycleScope.launch {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Authenticated -> {
                            LoadingDialog.hide(supportFragmentManager)
                            when (state.role) {
                                "USER" -> startActivity(
                                    Intent(this@ConnectionActivity, MainActivity::class.java)
                                )
                                "ADMIN", "SUPER_ADMIN" -> startActivity(
                                    Intent(this@ConnectionActivity, DashboardAdminActivity::class.java)
                                )
                                else -> toast("Rôle inconnu : ${state.role}")
                            }
                            finish()
                        }

                        is AuthUiState.EmailNotVerified -> {
                            LoadingDialog.hide(supportFragmentManager)
                            EmailSentDialogFragment.show(
                                fm   = supportFragmentManager,
                                mode = EmailSentDialogFragment.MODE_NOT_VERIFIED
                            )
                        }

                        is AuthUiState.Error -> {
                            LoadingDialog.hide(supportFragmentManager)
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

        // ── CGU ───────────────────────────────────────────────────────────────────

        private fun isCguAccepted(): Boolean =
            getSharedPreferences(CguDialogFragment.PREF_FILE, MODE_PRIVATE)
                .getBoolean(CguDialogFragment.PREF_KEY, false)

        private fun isPrivacyPolicyAccepted(): Boolean =
            getSharedPreferences(PrivacyPolicyDialogFragment.PREF_FILE, MODE_PRIVATE)
                .getBoolean(PrivacyPolicyDialogFragment.PREF_KEY, false)

        private fun showCguDialog(onAccepted: () -> Unit) {
            CguDialogFragment.show(
                fm         = supportFragmentManager,
                onAccepted = onAccepted,
                onDeclined = { toast("Vous devez accepter les CGU pour utiliser l'application") }
            )
        }

        private fun showPrivacyPolicyDialog(onAccepted: () -> Unit){
            PrivacyPolicyDialogFragment.show(
                fm         = supportFragmentManager,
                onAccepted = onAccepted,
                onDeclined = { toast("Vous devez accepter la politique de confidentialité pour utiliser l'application") }
            )
        }
    }