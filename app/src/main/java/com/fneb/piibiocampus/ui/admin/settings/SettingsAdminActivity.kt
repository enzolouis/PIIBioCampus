package com.fneb.piibiocampus.ui.admin.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.ExportDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.ui.auth.ConnectionActivity
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton
import kotlinx.coroutines.launch

class SettingsAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        setTopBarTitle(R.string.titleSettings)
        showTopBarLeftButton { finish() }

        findViewById<View>(R.id.rowEditProfileAdmin).setOnClickListener {
            startActivity(Intent(this, EditProfileActivityAdmin::class.java))
        }

        findViewById<View>(R.id.rowSaveAdmin).setOnClickListener {
            launchExport()
        }

        findViewById<View>(R.id.rowDeconnexionAdmin).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Es-tu sûr de vouloir te déconnecter ?")
                .setPositiveButton("Déconnexion") { _, _ ->
                    UserDao.signOut()
                    val intent = Intent(this, ConnectionActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        findViewById<View>(R.id.rowCguConfidentialiteAdmin).setOnClickListener {
            startActivity(Intent(this, CguActivityAdmin::class.java))
        }

        lifecycleScope.launch {
            val user = UserDao.getCurrentUserProfile()
            val deleteRow = findViewById<View>(R.id.rowDeleteAccountAdmin)

            if (user?.role == "SUPER_ADMIN") {
                deleteRow.visibility = View.GONE
            } else {
                deleteRow.setOnClickListener {
                    showDeleteDialog()
                }
            }
        }
    }

    private fun showDeleteDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Supprimer mon compte")
            .setMessage("Tu es sur le point de supprimer définitivement ton compte...")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    try {
                        UserDao.deleteCurrentUser()
                        val intent = Intent(this@SettingsAdminActivity, ConnectionActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SettingsAdminActivity,
                            "Erreur : ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun launchExport() {
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("Préparation de l'export…")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            val uid = UserDao.getCurrentUser()?.uid ?: run {
                progressDialog.dismiss()
                return@launch
            }

            try {
                ExportDao.exportUserDataAsCsv(this@SettingsAdminActivity, uid)
            } catch (e: Exception) {
                Toast.makeText(this@SettingsAdminActivity, "Erreur lors de l'export", Toast.LENGTH_SHORT).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }
}