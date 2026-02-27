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

    /**
     * Dernier nœud sur lequel l'utilisateur a cliqué pour naviguer dans l'arbre.
     * Utilisé par le bouton Stop : si aucune espèce n'est sélectionnée, on sauvegarde
     * ce nœud comme recensement partiel.
     * - Mis à jour à chaque appel à [navigateTo].
     * - Remis au parent (dernier de pathStack) lors d'un [navigateUp].
     * - Remis à null lors d'un [refreshRoots].
     */
    var lastNavigatedNodeId: String? = null
        private set

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
        lastNavigatedNodeId = null

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
        lastNavigatedNodeId = node.id   // ← mémorise le nœud de navigation
        _selectedNodeId.postValue(null)
        _currentNodes.postValue(node.children)
    }

    fun canNavigateUp(): Boolean = navStack.isNotEmpty()

    fun navigateUp() {
        if (canNavigateUp()) {
            val previous = navStack.removeLast()
            pathStack.removeLastOrNull()
            // Après être remonté, le "dernier navigué" est désormais le parent
            // (avant-dernier de pathStack, i.e. le nouveau dernier non-null)
            lastNavigatedNodeId = pathStack.lastOrNull { it != null }?.id
            _currentNodes.postValue(previous)
            _selectedNodeId.postValue(null)
        }
    }

    fun selectNode(node: CensusNode) {
        _selectedNodeId.postValue(node.id)
    }

    fun currentPath(): List<CensusNode> = pathStack.filterNotNull().toList()

    /**
     * Renvoie l'ID à utiliser pour une sauvegarde via le bouton Stop :
     * - Si une espèce est sélectionnée → on la prend (cas classique).
     * - Sinon → on prend le dernier nœud de navigation (ex : Passeriformes).
     * - Si on est encore à la racine → null.
     */
    fun stopCensusRef(): String? = _selectedNodeId.value ?: lastNavigatedNodeId

    /**
     * Cherche le chemin depuis la racine jusqu'au nœud [targetId]
     * et reconstruit la pile de navigation directement depuis les données,
     * sans passer par postValue (qui est asynchrone et rendrait _currentNodes.value
     * toujours vide au moment du addLast suivant).
     */
    private fun navigateToNodeById(targetId: String) {
        val roots = cachedRoots.ifEmpty { _currentNodes.value ?: return }
        val path  = findPathToNode(roots, targetId) ?: return

        // Réinitialise les piles
        navStack.clear()
        pathStack.clear()
        pathStack.addLast(null)
        lastNavigatedNodeId = null

        // Construit navStack et pathStack directement depuis les données du chemin.
        // On ne passe jamais par navigateTo() ni par _currentNodes.value ici,
        // car postValue est asynchrone : .value serait toujours vide pendant la boucle.
        //
        // Schéma pour un chemin [A, B, C] (C = cible feuille) :
        //   navStack  : [roots, A.children, B.children]
        //   pathStack : [null, A, B, C]  ← C ajouté seulement si non-feuille
        //   _currentNodes → B.children (niveau des frères de C)
        //   _selectedNodeId → C.id si feuille
        val target = path.last()
        val pathToParent = path.dropLast(1)

        var currentLevel: List<CensusNode> = roots
        for (node in pathToParent) {
            navStack.addLast(currentLevel)
            pathStack.addLast(node)
            lastNavigatedNodeId = node.id
            currentLevel = node.children
        }

        if (target.children.isEmpty()) {
            // Feuille (espèce) : affiche ses frères et la sélectionne
            _currentNodes.postValue(currentLevel)
            _selectedNodeId.postValue(target.id)
        } else {
            // Nœud intermédiaire : entre dedans et affiche ses enfants
            navStack.addLast(currentLevel)
            pathStack.addLast(target)
            lastNavigatedNodeId = target.id
            _currentNodes.postValue(target.children)
            _selectedNodeId.postValue(null)
        }
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