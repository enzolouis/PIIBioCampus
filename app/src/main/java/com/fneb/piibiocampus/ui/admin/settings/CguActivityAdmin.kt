package com.fneb.piibiocampus.ui.admin.settings

import android.os.Bundle
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
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.titleCgu)
        showTopBarLeftButton { finish() }
    }
}