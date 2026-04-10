package com.fneb.piibiocampus.ui.photo

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.data.ui.showError
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.ui.user.census.CensusMode
import com.fneb.piibiocampus.ui.user.census.CensusTreeActivity
import com.fneb.piibiocampus.ui.user.profiles.UserProfileFragment
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class PicturesViewerFragment : DialogFragment() {

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private val viewModel: PicturesViewerViewModel by viewModels {
        val state = arguments?.getParcelable<PhotoViewerState>(ARG_STATE)
            ?: error("PicturesViewerFragment: état manquant dans les arguments")
        PicturesViewerViewModel.factory(state)
    }

    // ── Vues ──────────────────────────────────────────────────────────────────

    private var ivAuthorProfile:    ImageView? = null
    private var tvTitle:            TextView?  = null
    private var tvValidationStatus: TextView?  = null
    private var photoView:          PhotoView? = null
    private var tvDate:             TextView?  = null
    private var tvDescription:      TextView?  = null
    private var btnResumeCensus:    Button?    = null
    private var btnValidate:        Button?    = null
    private var btnBack:            Button?    = null
    private var btnDelete:          Button?    = null
    private var dialogNetworkIcon: ImageView? = null

    // ── Launcher recensement ──────────────────────────────────────────────────

    private val censusLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) viewModel.reloadPicture()
        }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG       = "PicturesViewerFragment"
        private const val ARG_STATE = "state"
        const val RESULT_DELETED    = "picture_deleted"
        const val REQUEST_KEY       = "PicturesViewerFragment"

        fun show(fm: FragmentManager, state: PhotoViewerState) {
            PicturesViewerFragment().apply {
                arguments = Bundle().apply { putParcelable(ARG_STATE, state) }
            }.show(fm, TAG)
        }
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogPopup)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pictures_viewer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupClickListeners()
        observeViewModel()
        val activity = requireActivity() as? BaseActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                activity?.networkMonitor?.isOnline?.collect { online ->
                    updateDialogNetworkIcon(!online)
                }
            }
        }
    }

    private fun updateDialogNetworkIcon(show: Boolean) {
        val window = dialog?.window ?: return
        if (show) {
            if (dialogNetworkIcon == null) {
                dialogNetworkIcon = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_signal_wifi_off)
                    imageTintList = ColorStateList.valueOf(Color.parseColor("#E53935"))
                    val sizePx = (64 * resources.displayMetrics.density).toInt()
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                        gravity = Gravity.CENTER
                    }
                }
                (window.decorView as? ViewGroup)?.addView(dialogNetworkIcon)
            }
        } else {
            dialogNetworkIcon?.let { (it.parent as? ViewGroup)?.removeView(it) }
            dialogNetworkIcon = null
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dm = resources.displayMetrics
            window.setLayout((dm.widthPixels * 0.92f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ivAuthorProfile    = null
        tvTitle            = null
        tvValidationStatus = null
        photoView          = null
        tvDate             = null
        tvDescription      = null
        btnResumeCensus    = null
        btnValidate        = null
        btnBack            = null
        btnDelete          = null
    }

    // ── Initialisation des vues ───────────────────────────────────────────────

    private fun bindViews(view: View) {
        ivAuthorProfile    = view.findViewById(R.id.ivAuthorProfile)
        tvTitle            = view.findViewById(R.id.tvTitle)
        tvValidationStatus = view.findViewById(R.id.tvValidationStatus)
        photoView          = view.findViewById(R.id.photoView)
        tvDate             = view.findViewById(R.id.tvDate)
        tvDescription      = view.findViewById(R.id.tvDescription)
        btnResumeCensus    = view.findViewById(R.id.btnResumeCensus)
        btnValidate        = view.findViewById(R.id.btnValidate)
        btnBack            = view.findViewById(R.id.btnBack)
        btnDelete          = view.findViewById(R.id.btnDelete)
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        btnBack?.setOnClickListener { dismiss() }

        ivAuthorProfile?.setOnClickListener {
            val userRef = viewModel.uiState.value.userRef
            dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserProfileFragment.newInstance(userRef))
                .addToBackStack(null)
                .commit()
        }

        btnValidate?.setOnClickListener {
            val isValidated = viewModel.uiState.value.adminValidated
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmation")
                .setMessage(if (!isValidated) "Valider cette photo ?" else "Invalider cette photo ?")
                .setPositiveButton("Oui") { _, _ -> viewModel.validatePicture() }
                .setNegativeButton("Annuler", null)
                .show()
        }

        btnDelete?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer la photo")
                .setMessage("Cette action est irréversible. Confirmer la suppression ?")
                .setPositiveButton("Supprimer") { _, _ -> viewModel.deletePicture() }
                .setNegativeButton("Annuler", null)
                .show()
        }

        btnResumeCensus?.setOnClickListener {
            val state = viewModel.uiState.value
            val intent = Intent(requireContext(), CensusTreeActivity::class.java).apply {
                putExtra("mode",      CensusMode.UPDATE.name)
                putExtra("pictureId", state.pictureId)
                putExtra("imageUrl",  state.imageUrl)
                putExtra("caller",    state.caller.name)
                state.censusRef?.takeIf { it.isNotEmpty() && it != "null" }
                    ?.let { putExtra("initialNodeId", it) }
            }
            censusLauncher.launch(intent)
        }
    }

    // ── Observation du ViewModel ──────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch { viewModel.uiState.collect { state -> renderState(state) } }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is PicturesViewerEvent.ShowToast  ->
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            is PicturesViewerEvent.ShowError  ->
                                showError(event.exception)
                            is PicturesViewerEvent.PictureValidated ->
                                parentFragmentManager.setFragmentResult(
                                    REQUEST_KEY,
                                    Bundle().apply {
                                        putString("validated_picture_id", event.pictureId)
                                        putBoolean("validated_value", event.newValue)
                                    }
                                )
                            is PicturesViewerEvent.PictureDeleted ->
                                parentFragmentManager.setFragmentResult(
                                    REQUEST_KEY,
                                    Bundle().apply {
                                        putBoolean(RESULT_DELETED, true)
                                        putString("deleted_picture_id", event.pictureId)
                                    }
                                )
                            is PicturesViewerEvent.CensusUpdated ->
                                parentFragmentManager.setFragmentResult(
                                    REQUEST_KEY,
                                    Bundle().apply { putBoolean("census_updated", true) }
                                )
                            PicturesViewerEvent.Dismiss -> dismiss()
                        }
                    }
                }
            }
        }
    }

    // ── Rendu depuis l'état ───────────────────────────────────────────────────

    private fun renderState(state: PhotoViewerState) {
        photoView?.let {
            Picasso.get().load(state.imageUrl)
                .placeholder(R.drawable.photo_placeholder)
                .error(R.drawable.photo_placeholder)
                .into(it)
        }

        if (state.showAuthorProfile && !state.profilePictureUrl.isNullOrEmpty()) {
            ivAuthorProfile?.isVisible = true
            ivAuthorProfile?.let {
                Picasso.get().load(state.profilePictureUrl)
                    .placeholder(R.drawable.user_circle)
                    .transform(CircleTransform())
                    .into(it)
            }
        } else {
            ivAuthorProfile?.isVisible = false
        }

        tvTitle?.text = state.displayTitle
        tvDate?.text  = state.timestamp
        tvDescription?.text = buildString {
            appendLine("Famille : ${state.family ?: "Non identifiée"}")
            appendLine("Genre   : ${state.genre   ?: "Non identifié"}")
            append(    "Espèce  : ${state.specie  ?: "Non identifiée"}")
        }

        renderValidationStatus(state)

        btnValidate?.isVisible = state.showValidate
        if (state.showValidate) renderValidateButton(state)

        btnResumeCensus?.isVisible = state.showResumeCensus
        btnDelete?.isVisible       = state.showDelete
    }

    // ── Helpers de rendu ─────────────────────────────────────────────────────

    private fun renderValidationStatus(state: PhotoViewerState) {
        tvValidationStatus?.apply {
            isVisible = true
            when {
                state.adminValidated    -> { text = "✓ Validé par un administrateur";       setTextColor(Color.parseColor("#4CAF50")) }
                !state.recordingStatus  -> { text = "● Recensement non terminé";            setTextColor(Color.parseColor("#F44336")) }
                else                    -> { text = "○ Recensement non validé par un admin"; setTextColor(Color.parseColor("#FF9800")) }
            }
        }
    }

    private fun renderValidateButton(state: PhotoViewerState) {
        btnValidate?.apply {
            if (state.adminValidated) {
                text = "Invalider"
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
            } else {
                text = "Valider"
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
            }
        }
    }
}