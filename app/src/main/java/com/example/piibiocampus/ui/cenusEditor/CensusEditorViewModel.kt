package com.example.piibiocampus.ui.census

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.piibiocampus.data.dao.CensusDao
import java.util.UUID

class CensusEditorViewModel : ViewModel() {

    // ── Arbre complet en mémoire ───────────────────────────────────────────────
    private val allRoots = mutableListOf<CensusNode>()
    private var firestoreDocId: String? = null

    // ── Navigation ────────────────────────────────────────────────────────────
    private val navStack  = ArrayDeque<List<CensusNode>>()
    private val pathStack = ArrayDeque<CensusNode?>()

    // ── LiveData ──────────────────────────────────────────────────────────────
    private val _currentNodes = MutableLiveData<List<CensusNode>>(emptyList())
    val currentNodes: LiveData<List<CensusNode>> = _currentNodes

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // ── Chargement ────────────────────────────────────────────────────────────

    fun loadTree() {
        _isLoading.value = true
        CensusDao.fetchCensusTreeFull(
            onComplete = { docId, roots ->
                firestoreDocId = docId
                allRoots.clear()
                allRoots.addAll(roots)
                navStack.clear()
                pathStack.clear()
                pathStack.addLast(null)
                _currentNodes.postValue(allRoots.toList())
                _isLoading.postValue(false)
            },
            onError = { e ->
                _error.postValue(e.message)
                _isLoading.postValue(false)
            }
        )
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun navigateTo(node: CensusNode) {
        _currentNodes.value?.let { navStack.addLast(it) }
        pathStack.addLast(node)
        _currentNodes.postValue(node.children)
    }

    fun navigateUp() {
        if (canNavigateUp()) {
            _currentNodes.postValue(navStack.removeLast())
            pathStack.removeLastOrNull()
        }
    }

    fun canNavigateUp(): Boolean = navStack.isNotEmpty()
    fun currentPath(): List<CensusNode> = pathStack.filterNotNull()

    /**
     * Type que les nouveaux enfants doivent avoir au niveau courant.
     */
    fun childTypeForCurrentLevel(): CensusType {
        val parent = pathStack.lastOrNull { it != null }
        return when (parent?.type) {
            CensusType.ORDER  -> CensusType.FAMILY
            CensusType.FAMILY -> CensusType.GENUS
            CensusType.GENUS  -> CensusType.SPECIES
            else              -> CensusType.ORDER
        }
    }

    /** Le niveau courant peut-il encore avoir des enfants ? (SPECIES est une feuille) */
    fun canAddChildren(): Boolean = childTypeForCurrentLevel() != CensusType.SPECIES ||
            pathStack.lastOrNull { it != null }?.type != CensusType.GENUS.let { CensusType.SPECIES }

    // ── Mutations ─────────────────────────────────────────────────────────────

    fun addNode(name: String, description: List<String>, imageUrl: String) {
        val type = childTypeForCurrentLevel()
        val newNode = CensusNode(
            id          = UUID.randomUUID().toString(),
            name        = name,
            imageUrl    = imageUrl,
            description = description,
            type        = type,
            children    = emptyList()
        )
        val parent = pathStack.lastOrNull { it != null }
        if (parent == null) {
            allRoots.add(newNode)
            _currentNodes.postValue(allRoots.toList())
        } else {
            val updatedParent = parent.copy(children = parent.children + newNode)
            applyNodeUpdate(parent, updatedParent)
            _currentNodes.postValue(updatedParent.children)
        }
        persistTree()
    }

    fun updateNode(original: CensusNode, name: String, description: List<String>, imageUrl: String) {
        val updated = original.copy(name = name, description = description, imageUrl = imageUrl)
        applyNodeUpdate(original, updated)
        _currentNodes.postValue(
            _currentNodes.value?.map { if (it.id == original.id) updated else it } ?: emptyList()
        )
        persistTree()
    }

    fun deleteNode(node: CensusNode) {
        val parent = pathStack.lastOrNull { it != null }
        if (parent == null) {
            allRoots.removeAll { it.id == node.id }
            _currentNodes.postValue(allRoots.toList())
        } else {
            val updatedParent = parent.copy(children = parent.children.filter { it.id != node.id })
            applyNodeUpdate(parent, updatedParent)
            _currentNodes.postValue(updatedParent.children)
        }
        persistTree()
    }

    /** Nombre total de descendants (pour l'alerte de suppression) */
    fun countDescendants(node: CensusNode): Int =
        node.children.sumOf { 1 + countDescendants(it) }

    // ── Helpers internes ──────────────────────────────────────────────────────

    /**
     * Remplace [original] par [updated] dans allRoots, navStack et pathStack.
     */
    private fun applyNodeUpdate(original: CensusNode, updated: CensusNode) {
        // allRoots
        for (i in allRoots.indices) {
            allRoots[i] = replaceInTree(allRoots[i], original.id, updated)
        }
        // navStack : chaque liste mémorisée
        for (i in navStack.indices) {
            navStack[i] = navStack[i].map { if (it.id == original.id) updated else it }
        }
        // pathStack : le nœud parent peut y figurer
        for (i in pathStack.indices) {
            if (pathStack[i]?.id == original.id) pathStack[i] = updated
        }
    }

    private fun replaceInTree(node: CensusNode, targetId: String, replacement: CensusNode): CensusNode {
        if (node.id == targetId) return replacement
        return node.copy(children = node.children.map { replaceInTree(it, targetId, replacement) })
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    private fun persistTree() {
        CensusDao.saveCensusDocument(
            docId  = firestoreDocId,
            roots  = allRoots.toList(),
            onSuccess = { newDocId -> if (firestoreDocId == null) firestoreDocId = newDocId },
            onError   = { e -> _error.postValue("Erreur sauvegarde : ${e.message}") }
        )
    }
}
