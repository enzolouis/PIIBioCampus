package com.example.piibiocampus.ui.searchUsers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piibiocampus.R
import com.example.piibiocampus.data.dao.UserDao
import com.example.piibiocampus.data.model.UserProfile
import com.example.piibiocampus.ui.profiles.UserProfileFragment
import com.example.piibiocampus.utils.setTopBarTitle
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SearchUsersFragment : Fragment() {

    private lateinit var adapter: SearchUserAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText

    private val allUsers = mutableListOf<UserProfile>()
    private val displayedUsers = mutableListOf<UserProfile>()
    private var currentQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search_users, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTopBarTitle(R.string.txtSearchUsers)

        recyclerView   = view.findViewById(R.id.resultsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)

        adapter = SearchUserAdapter(displayedUsers) { user ->
            // Navigation vers le profil de l'utilisateur
            val fragment = UserProfileFragment.newInstance(user.uid)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadAllUsers()

        searchEditText.addTextChangedListener {
            val query = it.toString()
            if (query != currentQuery) {
                currentQuery = query
                applyFilter()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setTopBarTitle(R.string.txtSearchUsers)
    }

    private fun loadAllUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = UserDao.getAllUsers()
                allUsers.clear()
                allUsers.addAll(result)
                applyFilter()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (currentQuery.isBlank()) {
            allUsers.toList()
        } else {
            allUsers.filter {
                it.name?.lowercase()?.contains(currentQuery.lowercase()) == true
            }
        }
        displayedUsers.clear()
        displayedUsers.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}