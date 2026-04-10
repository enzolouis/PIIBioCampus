package com.fneb.piibiocampus.ui.auth

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.fneb.piibiocampus.R

/**
 * Dialog réutilisable pour les notifications liées à l'email de vérification.
 *
 * Deux usages :
 *  - MODE_ACCOUNT_CREATED : affiché juste après la création de compte
 *  - MODE_NOT_VERIFIED    : affiché quand l'utilisateur tente de se connecter
 *                           sans avoir vérifié son email
 */
class EmailSentDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "EmailSentDialogFragment"

        private const val ARG_MODE = "mode"

        const val MODE_ACCOUNT_CREATED = "account_created"
        const val MODE_NOT_VERIFIED    = "not_verified"

        fun show(
            fm: androidx.fragment.app.FragmentManager,
            mode: String,
            onDismiss: () -> Unit = {}
        ) {
            val fragment = EmailSentDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_MODE, mode) }
                this.onDismissCallback = onDismiss
            }
            fragment.show(fm, TAG)
        }
    }

    var onDismissCallback: () -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_email_sent, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mode    = arguments?.getString(ARG_MODE) ?: MODE_ACCOUNT_CREATED
        val title   = view.findViewById<TextView>(R.id.dialogTitle)
        val message = view.findViewById<TextView>(R.id.dialogMessage)
        val sender  = view.findViewById<TextView>(R.id.dialogSender)
        val btn     = view.findViewById<Button>(R.id.dialogBtn)

        when (mode) {
            MODE_ACCOUNT_CREATED -> {
                title.text   = "Compte créé !"
                message.text =
                    "Un email de vérification vient d'être envoyé à votre adresse (il peut se trouver dans les spams).\n\n" +
                    "Cliquez sur le lien dans l'email pour activer votre compte, " +
                    "puis revenez vous connecter."
                sender.text  = "Expéditeur : noreply@piibiocampus.fr"
            }
            MODE_NOT_VERIFIED -> {
                title.text   = "Email non vérifié"
                message.text =
                    "Votre adresse email n'est pas encore vérifiée.\n\n" +
                    "Un nouvel email de vérification vient d'être envoyé automatiquement. " +
                    "Consultez votre boîte mail (et vos spams) et cliquez sur le lien, " +
                    "puis reconnectez-vous."
                sender.text  = "Expéditeur : noreply@piibiocampus.fr"
            }
        }

        btn.setOnClickListener {
            dismiss()
            onDismissCallback()
        }

        isCancelable = false
    }

    override fun onStart() {
        super.onStart()
        // Arrondir les coins du dialog
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.drawable.fields_round_border)
    }
}
