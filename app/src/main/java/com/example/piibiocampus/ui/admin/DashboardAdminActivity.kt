package com.example.piibiocampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.piibiocampus.R
import com.example.piibiocampus.utils.setTopBarTitle

class DashboardAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)
        setTopBarTitle(R.string.titleAdmin);

        val btnSearch: Button = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener {
            val intent = Intent(this, SearchUsersAdminActivity::class.java)
            startActivity(intent)
        }

        val btnLstCensus: Button = findViewById(R.id.btnLstCensus);
        btnLstCensus.setOnClickListener{
            val intent = Intent(this, PicturesAdminActivity::class.java)
            startActivity(intent)
        }

    }
}