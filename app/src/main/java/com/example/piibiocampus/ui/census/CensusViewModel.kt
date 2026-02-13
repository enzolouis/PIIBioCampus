package com.example.piibiocampus.ui.census

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.piibiocampus.data.repository.CensusRepository

class CensusViewModel(private val repository: CensusRepository) : ViewModel() {

    // la liste d'éléments actuellement affichée (ex: listes d'ordres, puis familles, ...)
    private val _currentNodes = MutableLiveData<List<CensusNode>>(emptyList())
    val currentNodes: LiveData<List<CensusNode>> = _currentNodes

    // stack de navigation (permet de revenir en arrière)
    private val navStack = ArrayDeque<List<CensusNode>>()
    private val pathStack = ArrayDeque<CensusNode?>() // noeuds parent correspondants (nullable root)

    init {
        repository.loadRoots { /* erreur -> loger ou exposer un LiveData d'erreur si besoin */ }
        repository.roots.observeForever { roots ->
            // Au premier chargement, affiche les ordres
            if (roots.isNotEmpty() && _currentNodes.value.isNullOrEmpty()) {
                // Clear stacks et push roots
                navStack.clear()
                pathStack.clear()
                _currentNodes.postValue(roots)
                // parent null pour racine
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
        // affiche les enfants (peut être vide)
        _currentNodes.postValue(node.children)
    }

    fun canNavigateUp(): Boolean = navStack.isNotEmpty()

    fun navigateUp() {
        if (canNavigateUp()) {
            val previous = navStack.removeLast()
            pathStack.removeLastOrNull()
            _currentNodes.postValue(previous)
        }
    }

    // expose un helper pour récupérer le chemin (utile si tu veux afficher breadcrumb)
    fun currentPath(): List<CensusNode> = pathStack.filterNotNull().toList()
}
