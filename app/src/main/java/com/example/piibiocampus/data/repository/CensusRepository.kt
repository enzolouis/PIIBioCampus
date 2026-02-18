package com.example.piibiocampus.data.repository


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.piibiocampus.data.dao.CensusDao
import com.example.piibiocampus.ui.census.CensusNode

class CensusRepository(private val dao: CensusDao) {

    private val _roots = MutableLiveData<List<CensusNode>>()
    val roots: LiveData<List<CensusNode>> = _roots

    fun loadRoots(onError: ((Exception) -> Unit)? = null) {
        dao.fetchCensusTree({ list ->
            _roots.postValue(list)
        }, { e ->
            onError?.invoke(e)
        })
    }
}
