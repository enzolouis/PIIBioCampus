package com.fneb.piibiocampus.ui.admin.settings.cgu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.utils.setTopBarTitle
import com.fneb.piibiocampus.utils.showTopBarLeftButton

class CguActivityAdmin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_cgu_pdc)

        setTopBarTitle(R.string.titleCgu)
        showTopBarLeftButton { finish() }

        val btnLinkPolicy: Button = findViewById(R.id.btnLinkPolicy)

        btnLinkPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://piibiocampus.fneb.fr/politique-de-confidentialite")

            btnLinkPolicy.context.startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleCgu)
        showTopBarLeftButton { finish() }
    }
}