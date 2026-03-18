package com.example.piibiocampus.ui.common

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.piibiocampus.R

class LoadingDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_loading, container, false)
        val message = arguments?.getString(ARG_MESSAGE)
        view.findViewById<TextView>(R.id.tvLoadingMessage).apply {
            text       = message ?: "Chargement…"
            visibility = if (message != null) View.VISIBLE else View.GONE
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        private const val TAG         = "LoadingDialog"
        private const val ARG_MESSAGE = "message"

        fun show(fm: FragmentManager, message: String? = null): LoadingDialog {
            // Évite les doubles affichages
            (fm.findFragmentByTag(TAG) as? LoadingDialog)?.let { return it }

            return LoadingDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
                show(fm, TAG)
            }
        }

        fun hide(fm: FragmentManager) {
            (fm.findFragmentByTag(TAG) as? LoadingDialog)?.dismissAllowingStateLoss()
        }
    }
}
