package com.example.piibiocampus.ui.census

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.piibiocampus.data.repository.CensusRepository
import com.example.piibiocampus.data.dao.CensusDao

class CensusViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CensusViewModel::class.java)) {
            val dao = CensusDao
            val repo = CensusRepository(dao)
            @Suppress("UNCHECKED_CAST")
            return CensusViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
