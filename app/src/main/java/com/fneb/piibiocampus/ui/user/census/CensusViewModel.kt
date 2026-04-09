package com.fneb.piibiocampus.ui.user.census

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fneb.piibiocampus.data.dao.PictureDao
import com.fneb.piibiocampus.data.error.AppException
import com.fneb.piibiocampus.data.error.FirebaseExceptionMapper
import com.fneb.piibiocampus.data.model.LocationMeta
import com.fneb.piibiocampus.data.repository.CensusRepository
import com.fneb.piibiocampus.data.ui.UiState

class CensusViewModel(private val repository: CensusRepository) : ViewModel() {

    // ── Arbre de navigation ───────────────────────────────────────────────────

    private val _currentNodes = MutableLiveData<List<CensusNode>>(emptyList())
    val currentNodes: LiveData<List<CensusNode>> = _currentNodes

    private val _selectedNodeId = MutableLiveData<String?>(null)
    val selectedNodeId: LiveData<String?> = _selectedNodeId

    // ── États UI ──────────────────────────────────────────────────────────────

    /** Chargement de l'arbre (Loading / Success / Error) */
    val treeState: LiveData<UiState<List<CensusNode>>> = repository.uiState

    /** Résultat de la sauvegarde photo (Loading / Success / Error) */
    private val _saveState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val saveState: LiveData<UiState<Unit>> = _saveState

    // ── Navigation interne ────────────────────────────────────────────────────

    private val navStack  = ArrayDeque<List<CensusNode>>()
    private val pathStack = ArrayDeque<CensusNode?>()
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

    // ── Chargement ────────────────────────────────────────────────────────────

    /**
     * Recharge les racines depuis le repository.
     * Si [initialNodeId] est fourni, navigue automatiquement jusqu'à ce nœud
     * une fois les données chargées (utilisé pour "Reprendre le recensement").
     */
    fun refreshRoots(initialNodeId: String? = null) {
        navStack.clear()
        pathStack.clear()
        pathStack.addLast(null)
        _selectedNodeId.value = null
        _currentNodes.value = emptyList()
        cachedRoots = emptyList()
        lastNavigatedNodeId = null

        repository.loadRoots()

        if (initialNodeId != null) {
            val observer = object : androidx.lifecycle.Observer<List<CensusNode>> {
                override fun onChanged(value: List<CensusNode>) {
                    if (value.isNotEmpty()) {
                        cachedRoots = value
                        navStack.clear()
                        pathStack.clear()
                        pathStack.addLast(null)
                        _currentNodes.postValue(value)
                        navigateToNodeById(initialNodeId)
                        repository.roots.removeObserver(this)
                    }
                }
            }
            repository.roots.observeForever(observer)
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun navigateTo(node: CensusNode) {
        _currentNodes.value?.let { navStack.addLast(it) }
        pathStack.addLast(node)
        lastNavigatedNodeId = node.id
        _selectedNodeId.postValue(null)
        _currentNodes.postValue(node.children)
    }

    fun canNavigateUp(): Boolean = navStack.isNotEmpty()

    fun navigateUp() {
        if (canNavigateUp()) {
            val previous = navStack.removeLast()
            pathStack.removeLastOrNull()
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

    // ── Sauvegarde photo ──────────────────────────────────────────────────────

    fun createPicture(
        context: Context,
        imageBytes: ByteArray,
        location: LocationMeta,
        censusRef: String?,
        recordingStatus: Boolean
    ) {
        _saveState.value = UiState.Loading
        PictureDao.exportPictureFromBytes(
            context         = context,
            imageBytes      = imageBytes,
            location        = location,
            censusRef       = censusRef,
            recordingStatus = recordingStatus,
            adminValidated  = false,
            onSuccess       = {
                _saveState.postValue(UiState.Success(Unit))
            },
            onError = { e ->
                val mapped = e as? AppException ?: FirebaseExceptionMapper.map(e)
                _saveState.postValue(UiState.Error(mapped))
            }
        )
    }

    fun updatePicture(
        pictureId: String,
        censusRef: String?,
        recordingStatus: Boolean
    ) {
        _saveState.value = UiState.Loading
        PictureDao.updatePictureCensus(
            pictureId       = pictureId,
            censusRef       = censusRef,
            recordingStatus = recordingStatus,
            onSuccess       = {
                _saveState.postValue(UiState.Success(Unit))
            },
            onError = { e ->
                val mapped = e as? AppException ?: FirebaseExceptionMapper.map(e)
                _saveState.postValue(UiState.Error(mapped))
            }
        )
    }

    // ── Navigation par ID (reprise de recensement) ────────────────────────────

    /**
     * Cherche le chemin depuis la racine jusqu'au nœud [targetId]
     * et reconstruit la pile de navigation directement depuis les données,
     * sans passer par postValue (qui est asynchrone et rendrait _currentNodes.value
     * toujours vide au moment du addLast suivant).
     */
    private fun navigateToNodeById(targetId: String) {
        val roots = cachedRoots.ifEmpty { _currentNodes.value ?: return }
        val path  = findPathToNode(roots, targetId) ?: return

        navStack.clear()
        pathStack.clear()
        pathStack.addLast(null)
        lastNavigatedNodeId = null

        val target       = path.last()
        val pathToParent = path.dropLast(1)

        var currentLevel: List<CensusNode> = roots
        for (node in pathToParent) {
            navStack.addLast(currentLevel)
            pathStack.addLast(node)
            lastNavigatedNodeId = node.id
            currentLevel = node.children
        }

        if (target.children.isEmpty()) {
            _currentNodes.postValue(currentLevel)
            _selectedNodeId.postValue(target.id)
        } else {
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