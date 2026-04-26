package com.fneb.piibiocampus.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.ui.MainActivity
import com.fneb.piibiocampus.ui.admin.DashboardAdminActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        viewModel.checkCurrentUserAndFetchRoleIfNeeded()

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AuthUiState.Authenticated -> {
                        when (state.role) {
                            "USER" -> startActivity(
                                Intent(this@SplashActivity, MainActivity::class.java)
                            )
                            "ADMIN", "SUPER_ADMIN" -> startActivity(
                                Intent(this@SplashActivity, DashboardAdminActivity::class.java)
                                    .apply { putExtra("role", state.role) }
                            )
                        }
                        finish()
                    }
                    is AuthUiState.EmailNotVerified,
                    is AuthUiState.Idle,
                    is AuthUiState.Error -> {
                        startActivity(Intent(this@SplashActivity, ConnectionActivity::class.java))
                        finish()
                    }
                    else -> Unit // Loading → on attend
                }
            }
        }
    }
}