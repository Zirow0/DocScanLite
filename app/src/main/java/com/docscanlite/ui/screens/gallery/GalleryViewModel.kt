package com.docscanlite.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.domain.model.Document
import com.docscanlite.domain.usecase.DeleteDocumentUseCase
import com.docscanlite.domain.usecase.GetAllDocumentsUseCase
import com.docscanlite.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Gallery Screen
 * Manages document list and gallery state
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDocuments()
    }

    /**
     * Load all documents from repository
     */
    private fun loadDocuments() {
        viewModelScope.launch {
            try {
                getAllDocumentsUseCase().collect { documents ->
                    _uiState.value = if (documents.isEmpty()) {
                        GalleryUiState.Empty
                    } else {
                        GalleryUiState.Success(documents)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(
                    e.message ?: "Failed to load documents"
                )
            }
        }
    }

    /**
     * Search documents by query
     */
    fun search(query: String) {
        _searchQuery.value = query

        searchJob?.cancel()

        if (query.isBlank()) {
            loadDocuments()
            return
        }

        searchJob = viewModelScope.launch {
            try {
                searchDocumentsUseCase(query).collect { documents ->
                    _uiState.value = if (documents.isEmpty()) {
                        GalleryUiState.Empty
                    } else {
                        GalleryUiState.Success(documents)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = GalleryUiState.Error(
                    e.message ?: "Failed to search documents"
                )
            }
        }
    }

    /**
     * Toggle search mode
     */
    fun toggleSearchMode() {
        _isSearchMode.value = !_isSearchMode.value
        if (!_isSearchMode.value) {
            _searchQuery.value = ""
            loadDocuments()
        }
    }

    /**
     * Delete document
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            deleteDocumentUseCase(documentId)
            // Documents will auto-refresh via Flow
        }
    }

    /**
     * Refresh document list
     */
    fun refresh() {
        _searchQuery.value = ""
        _isSearchMode.value = false
        loadDocuments()
    }
}

/**
 * UI State for Gallery Screen
 */
sealed class GalleryUiState {
    data object Loading : GalleryUiState()
    data object Empty : GalleryUiState()
    data class Success(val documents: List<Document>) : GalleryUiState()
    data class Error(val message: String) : GalleryUiState()
}
