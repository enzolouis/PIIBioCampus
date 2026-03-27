package com.fneb.piibiocampus.ui

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

abstract class PermissionFragment : Fragment() {

    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted?.invoke()
        else onPermissionDenied?.invoke() ?: onDefaultPermissionDenied()
    }

    fun withPermission(
        permission: String,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {

            onPermissionGranted = onGranted
            onPermissionDenied = onDenied

            if (permission == android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                onGranted()
                return
            }

            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                requestPermissionLauncher.launch(permission)
            }

    }

    open fun onDefaultPermissionDenied() {
        android.widget.Toast.makeText(requireContext(), "Permission refusée", android.widget.Toast.LENGTH_SHORT).show()
    }
}