package com.fneb.piibiocampus.ui.census

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fneb.piibiocampus.data.dao.CensusDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.ui.user.census.CensusNode
import com.fneb.piibiocampus.ui.user.census.CensusType
import java.util.UUID

class CensusEditorViewModel : ViewModel() {

    // ── Arbre complet en mémoire ──────────────────────────────────────────────
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

    // AppException au lieu de String : on conserve userMessage et le type d'erreur
    private val _error = MutableLiveData<AppException?>(null)
    val error: LiveData<AppException?> = _error

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
                // fetchCensusTreeFull → onError(AppException) : pas de cast nécessaire
                _error.postValue(e)
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

    fun childTypeForCurrentLevel(): CensusType {
        val parent = pathStack.lastOrNull { it != null }
        return when (parent?.type) {
            CensusType.ORDER  -> CensusType.FAMILY
            CensusType.FAMILY -> CensusType.GENUS
            CensusType.GENUS  -> CensusType.SPECIES
            else              -> CensusType.ORDER
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    fun addNode(name: String, description: List<String>, imageUrl: String) {
        val type    = childTypeForCurrentLevel()
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

    fun countDescendants(node: CensusNode): Int =
        node.children.sumOf { 1 + countDescendants(it) }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private fun applyNodeUpdate(original: CensusNode, updated: CensusNode) {
        for (i in allRoots.indices) {
            allRoots[i] = replaceInTree(allRoots[i], original.id, updated)
        }
        for (i in navStack.indices) {
            navStack[i] = navStack[i].map { if (it.id == original.id) updated else it }
        }
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
            docId     = firestoreDocId,
            roots     = allRoots.toList(),
            onSuccess = { newDocId -> if (firestoreDocId == null) firestoreDocId = newDocId },
            onError   = { e ->
                // saveCensusDocument → onError(Exception) : on mappe vers AppException
                val appException = (e as? AppException) ?: FirebaseExceptionMapper.map(e)
                _error.postValue(appException)
            }
        )
    }
}