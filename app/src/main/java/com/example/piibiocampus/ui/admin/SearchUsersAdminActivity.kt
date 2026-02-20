package com.example.piibiocampus.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.UserDao
import com.example.piibiocampus.data.model.UserProfile
import com.example.piibiocampus.utils.setTopBarTitle
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SearchUsersAdminActivity : AppCompatActivity() {

    private lateinit var adapter: UserAdapter
    private val users = mutableListOf<UserProfile>()

    private var lastName: String? = null
    private var currentQuery: String = ""
    private val pageSize = 20L

    private lateinit var btnLoadMore: MaterialButton
    private lateinit var searchEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searchusers_admin)
        setTopBarTitle(R.string.txtSearchAdmin);

        val recyclerView = findViewById<RecyclerView>(R.id.resultsRecyclerView)
        btnLoadMore = findViewById(R.id.btnLoadMore)
        searchEditText = findViewById(R.id.searchEditText)

        adapter = UserAdapter(users) { user ->
            Toast.makeText(this, "Bannir ${user.name}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadFirstPage()

        searchEditText.addTextChangedListener {
            val query = it.toString()
            if (query != currentQuery) {
                currentQuery = query
                resetAndSearch()
            }
        }

        btnLoadMore.setOnClickListener {
            loadNextPage()
        }
    }

    // initial load
    private fun loadFirstPage() {
        lifecycleScope.launch {
            val result = UserDao.getUsersPage(pageSize)
            users.clear()
            users.addAll(result)
            adapter.notifyDataSetChanged()

            lastName = users.lastOrNull()?.name
            btnLoadMore.visibility = if (result.size < pageSize) View.GONE else View.VISIBLE
        }
    }

    // reset when search change
    private fun resetAndSearch() {
        lifecycleScope.launch {
            users.clear()
            adapter.notifyDataSetChanged()
            lastName = null

            if (currentQuery.isBlank()) {
                loadFirstPage()
                return@launch
            }

            val result = UserDao.searchUsers(currentQuery, pageSize)
            users.addAll(result)
            adapter.notifyDataSetChanged()

            lastName = users.lastOrNull()?.name
            btnLoadMore.visibility = if (result.size < pageSize) View.GONE else View.VISIBLE
        }
    }

    // paginating (see more)
    private fun loadNextPage() {
        val cursor = lastName ?: return

        lifecycleScope.launch {
            val result = if (currentQuery.isBlank()) {
                UserDao.getUsersPageAfter(cursor, pageSize)
            } else {
                UserDao.searchUsersAfter(currentQuery, cursor, pageSize)
            }

            val start = users.size
            users.addAll(result)
            adapter.notifyItemRangeInserted(start, result.size)

            lastName = users.lastOrNull()?.name
            btnLoadMore.visibility = if (result.size < pageSize) View.GONE else View.VISIBLE
        }
    }
}
