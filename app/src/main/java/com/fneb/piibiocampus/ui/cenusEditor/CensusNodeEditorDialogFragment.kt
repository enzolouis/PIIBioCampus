package com.fneb.piibiocampus.ui.census

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.dao.PictureDao
import com.squareup.picasso.Picasso

/**
 * Popup d'ajout / modification d'un nœud de l'arbre de recensement.
 *
 * Résultat via [onSave] : (name, description, imageUrl)
 * Si une nouvelle image a été choisie, elle est uploadée avant d'appeler onSave.
 */
class CensusNodeEditorDialogFragment : DialogFragment() {

    // ── Arguments ─────────────────────────────────────────────────────────────
    private var existingNode: CensusNode? = null
    private var nodeType: CensusType = CensusType.ORDER
    private var onSave: ((name: String, description: List<String>, imageUrl: String) -> Unit)? = null

    // ── State ─────────────────────────────────────────────────────────────────
    private var pickedImageBytes: ByteArray? = null
    private var currentImageUrl:  String     = ""

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var etName:        EditText
    private lateinit var etDescription: EditText
    private lateinit var ivPreview:     ImageView
    private lateinit var btnPickImage:  Button
    private lateinit var btnSave:       Button
    private lateinit var btnCancel:     Button
    private lateinit var progressBar:   ProgressBar
    private lateinit var tvTitle:       TextView

    // ── Permission selon version Android ─────────────────────────────────────
    private val readPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    // ── Launcher permission ───────────────────────────────────────────────────
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                imagePicker.launch("image/*")
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission refusée : impossible d'accéder à la galerie",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ── Image picker ──────────────────────────────────────────────────────────
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            try {
                val stream = requireContext().contentResolver.openInputStream(uri)
                pickedImageBytes = stream?.readBytes()
                stream?.close()
                ivPreview.setImageURI(uri)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erreur lecture image : ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    companion object {
        private const val TAG = "CensusNodeEditorDialog"

        fun show(
            fm:     FragmentManager,
            node:   CensusNode?,
            type:   CensusType,
            onSave: (name: String, description: List<String>, imageUrl: String) -> Unit
        ) {
            CensusNodeEditorDialogFragment().also {
                it.existingNode = node
                it.nodeType     = node?.type ?: type
                it.onSave       = onSave
            }.show(fm, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogPopup)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_census_node_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle       = view.findViewById(R.id.tvEditorTitle)
        etName        = view.findViewById(R.id.etNodeName)
        etDescription = view.findViewById(R.id.etNodeDescription)
        ivPreview     = view.findViewById(R.id.ivNodeImagePreview)
        btnPickImage  = view.findViewById(R.id.btnPickImage)
        btnSave       = view.findViewById(R.id.btnSaveNode)
        btnCancel     = view.findViewById(R.id.btnCancelNode)
        progressBar   = view.findViewById(R.id.progressBarEditor)

        // Titre selon opération et type
        val typeLabel = when (nodeType) {
            CensusType.ORDER   -> "un ordre"
            CensusType.FAMILY  -> "une famille"
            CensusType.GENUS   -> "un genre"
            CensusType.SPECIES -> "une espèce"
        }
        val node = existingNode
        tvTitle.text = if (node == null) "Ajouter $typeLabel" else "Modifier ${node.name}"

        // Pré-remplissage si modification
        if (node != null) {
            etName.setText(node.name)
            etDescription.setText(node.description.joinToString("\n"))
            currentImageUrl = node.imageUrl
            if (node.imageUrl.isNotBlank()) {
                Picasso.get()
                    .load(node.imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(ivPreview)
            }
        }

        btnPickImage.setOnClickListener { requestGalleryAccess() }
        btnCancel.setOnClickListener    { dismiss() }
        btnSave.setOnClickListener      { handleSave() }
    }

    // ── Taille popup ──────────────────────────────────────────────────────────

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dm = resources.displayMetrics
            window.setLayout(
                (dm.widthPixels * 0.92f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    // ── Permission + galerie ──────────────────────────────────────────────────

    private fun requestGalleryAccess() {
        when {
            // Permission déjà accordée → ouvre directement la galerie
            ContextCompat.checkSelfPermission(requireContext(), readPermission)
                    == PackageManager.PERMISSION_GRANTED -> {
                imagePicker.launch("image/*")
            }
            // L'utilisateur a déjà refusé une fois → explique pourquoi on en a besoin
            shouldShowRequestPermissionRationale(readPermission) -> {
                Toast.makeText(
                    requireContext(),
                    "L'accès à la galerie est nécessaire pour choisir une image",
                    Toast.LENGTH_LONG
                ).show()
                permissionLauncher.launch(readPermission)
            }
            // Première demande
            else -> permissionLauncher.launch(readPermission)
        }
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private fun handleSave() {
        val name = etName.text.toString().trim()
        if (name.isBlank()) {
            etName.error = "Le nom est obligatoire"
            return
        }
        val description = etDescription.text.toString()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val bytes = pickedImageBytes
        if (bytes != null) {
            setLoading(true)
            PictureDao.uploadImageForCensus(
                context    = requireContext(),
                imageBytes = bytes,
                onSuccess  = { url ->
                    activity?.runOnUiThread {
                        setLoading(false)
                        onSave?.invoke(name, description, url)
                        dismiss()
                    }
                },
                onError = { e ->
                    activity?.runOnUiThread {
                        setLoading(false)
                        Toast.makeText(
                            requireContext(),
                            "Erreur upload : ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } else {
            onSave?.invoke(name, description, currentImageUrl)
            dismiss()
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility  = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled       = !loading
        btnPickImage.isEnabled  = !loading
        etName.isEnabled        = !loading
        etDescription.isEnabled = !loading
    }
}