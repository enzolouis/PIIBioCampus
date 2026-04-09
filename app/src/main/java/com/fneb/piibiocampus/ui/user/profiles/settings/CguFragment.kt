package com.fneb.piibiocampus.ui.user.profiles.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.utils.setTopBarTitle

class CguFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cgu_pdc, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTopBarTitle(R.string.titleCgu)

        val btnLinkPolicy: Button = view.findViewById(R.id.btnLinkPolicy)

        btnLinkPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://piibiocampus.fneb.fr/politique-de-confidentialite")

            btnLinkPolicy.context.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleCgu)
    }
}