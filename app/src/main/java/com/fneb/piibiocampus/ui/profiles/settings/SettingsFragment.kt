package com.fneb.piibiocampus.ui.profiles.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.ExportDao
import com.fneb.piibiocampus.data.dao.UserDao
import com.fneb.piibiocampus.ui.PermissionFragment
import com.fneb.piibiocampus.utils.setTopBarTitle
import kotlinx.coroutines.launch

class SettingsFragment : PermissionFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.rowEditProfile).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowSave).setOnClickListener {
            android.util.Log.d("SettingsFragment", "Click sauvegarde")
            withPermission(
                permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                onGranted = {
                    android.util.Log.d("SettingsFragment", "Permission granted")
                    launchExport()
                }
            )
        }

        view.findViewById<View>(R.id.rowDeconnexion).setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Déconnexion")
                .setMessage("Es-tu sûr de vouloir te déconnecter ?")
                .setPositiveButton("Déconnexion") { _, _ ->
                    UserDao.signOut()
                    val intent = android.content.Intent(requireContext(), com.fneb.piibiocampus.ui.auth.ConnectionActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        view.findViewById<View>(R.id.rowCguConfidentialite).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CguFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowDeleteAccount).setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Supprimer mon compte")
                .setMessage("Tu es sur le point de supprimer définitivement ton compte ainsi que toutes tes données associées (photos, observations…). Cette action est irréversible.")
                .setPositiveButton("Supprimer") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            UserDao.deleteCurrentUser()
                            val intent = android.content.Intent(requireContext(), com.fneb.piibiocampus.ui.auth.ConnectionActivity::class.java)
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsFragment", "Erreur suppression compte", e)
                            Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleSettings)
    }

    private fun launchExport() {
        val progressDialog = android.app.ProgressDialog(requireContext()).apply {
            setMessage("Préparation de l'export…")
            setCancelable(false)
            show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = UserDao.getCurrentUser()?.uid ?: run {
                progressDialog.dismiss()
                return@launch
            }
            try {
                ExportDao.exportUserDataAsCsv(requireContext(), uid)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur lors de l'export", Toast.LENGTH_SHORT).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }
}