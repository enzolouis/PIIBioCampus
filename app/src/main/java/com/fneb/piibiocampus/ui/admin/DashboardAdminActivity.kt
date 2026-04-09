package com.fneb.piibiocampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.ui.BaseActivity
import com.fneb.piibiocampus.ui.admin.exportdata.ExportDataActivity
import com.fneb.piibiocampus.ui.admin.settings.SettingsAdminActivity
import com.fneb.piibiocampus.ui.admin.news.NewsListAdminActivity
import com.fneb.piibiocampus.ui.admin.searchUsersAdmin.SearchUsersAdminActivity
import com.fneb.piibiocampus.ui.census.CensusEditorActivity
import com.fneb.piibiocampus.utils.setTopBarTitle

class DashboardAdminActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)
        setTopBarTitle(R.string.titleAdmin)

        val adminGrid = findViewById<GridLayout>(R.id.adminGrid)
        ViewCompat.setOnApplyWindowInsetsListener(adminGrid) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (20 * resources.displayMetrics.density).toInt()
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                systemBars.bottom + extraPadding
            )
            insets
        }
        val btnSearch: Button = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener {
            val role = intent.getStringExtra("role")
            val intent = Intent(this, SearchUsersAdminActivity::class.java)
            intent.putExtra("role", role)
            startActivity(intent)
        }

        val btnLstCensus: Button = findViewById(R.id.btnLstCensus);
        btnLstCensus.setOnClickListener{
            val intent = Intent(this, PicturesAdminActivity::class.java)
            startActivity(intent)
        }

        val btnCensusEdit: Button = findViewById(R.id.btnTree);
        btnCensusEdit.setOnClickListener{
            val intent = Intent(this, CensusEditorActivity::class.java)
            startActivity(intent)
        }

        val btnExportData: Button = findViewById(R.id.btnExport);
        btnExportData.setOnClickListener{
            val intent = Intent(this, ExportDataActivity::class.java)
            startActivity(intent)
        }

        val btnNews: Button = findViewById(R.id.btnNews);
        btnNews.setOnClickListener {
            val intent = Intent(this, NewsListAdminActivity::class.java)
            startActivity(intent)
        }

        val btnSettings: Button = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsAdminActivity::class.java)
            startActivity(intent)
        }
    }
}