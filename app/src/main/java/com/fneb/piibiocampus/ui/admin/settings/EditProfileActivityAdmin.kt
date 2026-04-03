package com.fneb.piibiocampus.ui.admin.settings

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import kotlinx.coroutines.launch

class EditProfileActivityAdmin : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editOldPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var editConfirmPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_edit_profile_admin)

        setTopBarTitle(R.string.titleEditProfile)
        showTopBarLeftButton { finish() }

        editName            = findViewById(R.id.editNameAdmin)
        editOldPassword     = findViewById(R.id.editOldPasswordAdmin)
        editNewPassword     = findViewById(R.id.editNewPasswordAdmin)
        editConfirmPassword = findViewById(R.id.editConfirmPasswordAdmin)

        findViewById<Button>(R.id.saveButtonAdmin).setOnClickListener {
            saveProfile()
        }

        loadCurrentProfile()
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleEditProfile)
    }

    private fun loadCurrentProfile() {
        lifecycleScope.launch {
            val user = UserDao.getCurrentUser() ?: return@launch
            val profile = UserDao.getCurrentUserProfile() ?: return@launch

            editName.setText(profile.name)
        }
    }

    private fun saveProfile() {
        val name            = editName.text.toString().trim()
        val oldPassword     = editOldPassword.text.toString()
        val newPassword     = editNewPassword.text.toString()
        val confirmPassword = editConfirmPassword.text.toString()

        if (name.isEmpty()) {
            editName.error = "Le pseudo ne peut pas être vide"
            return
        }

        val passwordChangeRequested = oldPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()

        if (passwordChangeRequested) {
            if (oldPassword.isEmpty()) {
                editOldPassword.error = "Entrez votre ancien mot de passe"
                return
            }
            if (newPassword.length < 6) {
                editNewPassword.error = "Le mot de passe doit faire au moins 6 caractères"
                return
            }
            if (newPassword != confirmPassword) {
                editConfirmPassword.error = "Les mots de passe ne correspondent pas"
                return
            }
        }

        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("Enregistrement…")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                UserDao.updateUserProfileName(name)

                if (passwordChangeRequested) {
                    UserDao.updatePassword(oldPassword, newPassword)
                }

                Toast.makeText(this@EditProfileActivityAdmin, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivityAdmin, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }
}