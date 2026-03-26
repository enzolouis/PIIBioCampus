package com.fneb.piibiocampus.politiqueDeConfidentialite

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.fneb.piibiocampus.R

/**
 * Popup des Conditions Générales d'Utilisation.
 *
 * Le bouton "J'accepte" reste désactivé tant que l'utilisateur·ice
 * n'a pas fait défiler le texte jusqu'en bas.
 * L'acceptation est mémorisée dans les SharedPreferences sous la clé
 * [PREF_KEY] — elle n'est donc demandée qu'une seule fois par installation.
 *
 * @param onAccepted  callback appelé quand l'utilisateur·ice accepte
 * @param onDeclined  callback appelé quand il ou elle refuse (ou ferme sans accepter)
 */
class PrivacyPolicyDialogFragment : DialogFragment() {

    private var onAccepted: (() -> Unit)? = null
    private var onDeclined: (() -> Unit)? = null

    companion object {
        private const val TAG = "PrivacyPolicyDialogFragment"
        const val PRIVAXY_POLICY_VERSION = 1
        const val PREF_FILE   = "piibiocampus_prefs"
        const val PREF_KEY    = "poc_accepted_v$PRIVAXY_POLICY_VERSION"

        fun show(
            fm:          FragmentManager,
            onAccepted:  () -> Unit,
            onDeclined:  () -> Unit = {}
        ) {
            PrivacyPolicyDialogFragment().also {
                it.onAccepted = onAccepted
                it.onDeclined = onDeclined
            }.show(fm, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogPopup)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_politique_de_confidentialite_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollView  = view.findViewById<ScrollView>(R.id.scrollViewPoC)
        val btnAccept   = view.findViewById<Button>(R.id.btnAcceptPoC)
        val btnDecline  = view.findViewById<Button>(R.id.btnDeclinePoC)
        val tvScrollHint = view.findViewById<TextView>(R.id.tvScrollHint)

        // Bouton désactivé par défaut
        btnAccept.isEnabled = false
        btnAccept.alpha     = 0.4f

        // Détection du bas du scroll
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollY    = scrollView.scrollY
            val height     = scrollView.height
            val totalScroll = scrollView.getChildAt(0).height
            val atBottom   = (scrollY + height) >= (totalScroll - 32) // 32px de marge

            if (atBottom) {
                btnAccept.isEnabled = true
                btnAccept.alpha     = 1f
                tvScrollHint.visibility = View.GONE
            }
        }

        btnAccept.setOnClickListener {
            // Sauvegarder l'acceptation dans les SharedPreferences
            requireContext()
                .getSharedPreferences(PREF_FILE, android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_KEY, true)
                .apply()

            dismiss()
            onAccepted?.invoke()
        }

        btnDecline.setOnClickListener {
            dismiss()
            onDeclined?.invoke()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val dm = resources.displayMetrics
            window.setLayout(
                (dm.widthPixels * 0.92f).toInt(),
                (dm.heightPixels * 0.82f).toInt()
            )
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onDeclined?.invoke()
    }
}
