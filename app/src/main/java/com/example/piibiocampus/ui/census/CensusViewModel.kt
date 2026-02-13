package com.example.piibiocampus.ui.census

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.piibiocampus.data.repository.CensusRepository
import com.example.piibiocampus.ui.census.CensusNode

class CensusViewModel(private val repository: CensusRepository) : ViewModel() {

    // la liste d'éléments actuellement affichée (ex: listes d'ordres, puis familles, ...)
    private val _currentNodes = MutableLiveData<List<CensusNode>>(emptyList())
    val currentNodes: LiveData<List<CensusNode>> = _currentNodes

    // selection actuelle (id du noeud)
    private val _selectedNodeId = MutableLiveData<String?>(null)
    val selectedNodeId: LiveData<String?> = _selectedNodeId

    // stack de navigation (permet de revenir en arrière)
    private val navStack = ArrayDeque<List<CensusNode>>()
    private val pathStack = ArrayDeque<CensusNode?>() // noeuds parent correspondants (nullable root)

    init {
        repository.loadRoots { /* erreur -> loger ou exposer un LiveData d'erreur si besoin */ }
        repository.roots.observeForever { roots ->
            // Au premier chargement, affiche les ordres
            if (roots.isNotEmpty() && _currentNodes.value.isNullOrEmpty()) {
                navStack.clear()
                pathStack.clear()
                _currentNodes.postValue(roots)
                pathStack.addLast(null)
            }
        }
    }

    fun refreshRoots() {
        repository.loadRoots()
    }

    fun navigateTo(node: CensusNode) {
        // push current list sur la pile
        _currentNodes.value?.let { navStack.addLast(it) }
        pathStack.addLast(node)
        // clear selection lors de la navigation vers un nouveau niveau
        _selectedNodeId.postValue(null)
        // affiche les enfants (peut être vide)
        _currentNodes.postValue(node.children)
    }

    fun canNavigateUp(): Boolean = navStack.isNotEmpty()

    fun navigateUp() {
        if (canNavigateUp()) {
            val previous = navStack.removeLast()
            pathStack.removeLastOrNull()
            _currentNodes.postValue(previous)
            // clear selection en remontant
            _selectedNodeId.postValue(null)
        }
    }

    fun selectNode(node: CensusNode) {
        // Si node est feuille (species) on conserve selection ; sinon on conserve la sélection comme dernier noeud choisi
        _selectedNodeId.postValue(node.id)
    }

    // expose un helper pour récupérer le chemin (utile si tu veux afficher breadcrumb)
    fun currentPath(): List<CensusNode> = pathStack.filterNotNull().toList()
}
