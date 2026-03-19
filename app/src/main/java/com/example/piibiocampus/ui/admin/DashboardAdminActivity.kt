package com.example.piibiocampus.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.GridLayout
import com.example.piibiocampus.R
import com.example.piibiocampus.ui.census.CensusEditorActivity
import com.example.piibiocampus.utils.DatabaseFiller.fillUsers
import com.example.piibiocampus.utils.setTopBarTitle

class DashboardAdminActivity : AppCompatActivity() {

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
            val intent = Intent(this, SearchUsersAdminActivity::class.java)
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
    }
}