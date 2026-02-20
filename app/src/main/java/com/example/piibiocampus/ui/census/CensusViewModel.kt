package com.example.piibiocampus.ui.census

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.piibiocampus.data.repository.CensusRepository

class CensusViewModel(private val repository: CensusRepository) : ViewModel() {

    private val _currentNodes = MutableLiveData<List<CensusNode>>(emptyList())
    val currentNodes: LiveData<List<CensusNode>> = _currentNodes

    private val _selectedNodeId = MutableLiveData<String?>(null)
    val selectedNodeId: LiveData<String?> = _selectedNodeId

    private val navStack  = ArrayDeque<List<CensusNode>>()
    private val pathStack = ArrayDeque<CensusNode?>()

    // Garde une référence aux racines pour pouvoir chercher un nœud par ID
    private var cachedRoots: List<CensusNode> = emptyList()

    init {
        repository.roots.observeForever { roots ->
            if (roots.isNotEmpty() && _currentNodes.value.isNullOrEmpty()) {
                cachedRoots = roots
                navStack.clear()
                pathStack.clear()
                pathStack.addLast(null)
                _currentNodes.postValue(roots)
            }
        }
        repository.loadRoots()
    }

    /**
     * Recharge les racines depuis le repository.
     * Si [initialNodeId] est fourni, navigue automatiquement jusqu'à ce nœud
     * une fois les données chargées (utilisé pour "Reprendre le recensement").
     */
    fun refreshRoots(initialNodeId: String? = null) {
        // Réinitialise la navigation
        navStack.clear()
        pathStack.clear()
        pathStack.addLast(null)
        _selectedNodeId.value = null
        _currentNodes.value = emptyList()
        cachedRoots = emptyList()

        repository.loadRoots(onError = { e ->
            // Tu peux exposer une LiveData d'erreur ici si besoin
        })

        if (initialNodeId != null) {
            // On observe une seule fois les roots pour lancer la navigation
            val observer = object : androidx.lifecycle.Observer<List<CensusNode>> {
                override fun onChanged(roots: List<CensusNode>) {
                    if (roots.isNotEmpty()) {
                        cachedRoots = roots
                        navStack.clear()
                        pathStack.clear()
                        pathStack.addLast(null)
                        _currentNodes.postValue(roots)

                        // Navigue jusqu'au nœud sauvegardé
                        navigateToNodeById(initialNodeId)

                        // Désinscription après le premier appel
                        repository.roots.removeObserver(this)
                    }
                }
            }
            repository.roots.observeForever(observer)
        }
    }
    fun navigateTo(node: CensusNode) {
        _currentNodes.value?.let { navStack.addLast(it) }
        pathStack.addLast(node)
        _selectedNodeId.postValue(null)
        _currentNodes.postValue(node.children)
    }

    fun canNavigateUp(): Boolean = navStack.isNotEmpty()

    fun navigateUp() {
        if (canNavigateUp()) {
            val previous = navStack.removeLast()
            pathStack.removeLastOrNull()
            _currentNodes.postValue(previous)
            _selectedNodeId.postValue(null)
        }
    }

    fun selectNode(node: CensusNode) {
        _selectedNodeId.postValue(node.id)
    }

    fun currentPath(): List<CensusNode> = pathStack.filterNotNull().toList()

    /**
     * Cherche le chemin depuis la racine jusqu'au nœud [targetId]
     * et rejoue la navigation pour reconstruire la pile.
     * Appelé uniquement après que les racines sont chargées.
     */
    private fun navigateToNodeById(targetId: String) {
        val roots = cachedRoots.ifEmpty { _currentNodes.value ?: return }
        val path  = findPathToNode(roots, targetId) ?: return

        // Réinitialise la pile avant de rejouer
        navStack.clear()
        pathStack.clear()
        pathStack.addLast(null)

        // Rejoue la navigation jusqu'à l'avant-dernier nœud
        for (node in path.dropLast(1)) {
            _currentNodes.value?.let { navStack.addLast(it) }
            pathStack.addLast(node)
            _currentNodes.postValue(node.children)
        }

        // Sélectionne le nœud final (feuille / espèce)
        path.lastOrNull()?.let { selectNode(it) }
    }

    /**
     * Recherche récursive : retourne le chemin de nœuds depuis la racine
     * jusqu'au nœud cible, ou null si introuvable.
     */
    private fun findPathToNode(
        nodes: List<CensusNode>,
        targetId: String,
        currentPath: List<CensusNode> = emptyList()
    ): List<CensusNode>? {
        for (node in nodes) {
            val path = currentPath + node
            if (node.id == targetId) return path
            val found = findPathToNode(node.children, targetId, path)
            if (found != null) return found
        }
        return null
    }
}