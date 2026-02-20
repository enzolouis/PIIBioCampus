package com.example.piibiocampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.piibiocampus.R

class DashboardAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        val btn: Button = findViewById(R.id.btnSearch);
        btn.setOnClickListener {
            val intent = Intent(this, SearchUsersAdminActivity::class.java)
            startActivity(intent)
        }
    }
}