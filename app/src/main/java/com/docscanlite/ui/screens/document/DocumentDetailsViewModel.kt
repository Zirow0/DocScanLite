package com.docscanlite.ui.screens.document

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.ImageUtils
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import com.docscanlite.domain.usecase.ProcessImageUseCase
import com.docscanlite.export.image.ImageExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Document Details Screen
 * Manages document viewing and editing
 */
@HiltViewModel
class DocumentDetailsViewModel @Inject constructor(
    application: Application,
    private val documentRepository: DocumentRepository,
    private val processImageUseCase: ProcessImageUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<DocumentDetailsUiState>(DocumentDetailsUiState.Loading)
    val uiState: StateFlow<DocumentDetailsUiState> = _uiState.asStateFlow()

    /**
     * Load document by ID
     */
    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.value = DocumentDetailsUiState.Loading

            when (val result = documentRepository.getDocumentById(documentId)) {
                is Result.Success -> {
                    _uiState.value = DocumentDetailsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = DocumentDetailsUiState.Error(
                        result.exception.message ?: "Failed to load document"
                    )
                }
                is Result.Loading -> {
                    // Already in Loading state
                }
            }
        }
    }

    /**
     * Delete document
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            when (documentRepository.deleteDocument(documentId)) {
                is Result.Success -> {
                    // Document deleted successfully
                    // Navigation back will be handled by the screen
                }
                is Result.Error -> {
                    // Show error (will be enhanced with Snackbar later)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Share document image via system share sheet
     */
    fun shareDocument(): Intent? {
        val currentState = _uiState.value
        if (currentState !is DocumentDetailsUiState.Success) {
            return null
        }

        val document = currentState.document
        val imagePath = document.processedPath
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            return null
        }

        val exporter = ImageExporter(getApplication())
        return exporter.shareImage(imageFile)
    }

    /**
     * Process document with OCR to extract text
     */
    fun processDocumentWithOcr(document: Document) {
        viewModelScope.launch {
            _uiState.value = DocumentDetailsUiState.Loading

            try {
                withContext(Dispatchers.IO) {
                    val imageFile = File(document.originalPath)
                    val bitmap = ImageUtils.loadBitmapFromFile(imageFile)

                    bitmap?.let {
                        when (val ocrResult = processImageUseCase(it)) {
                            is Result.Success -> {
                                // Update document with OCR text
                                val updatedDocument = document.copy(
                                    ocrText = ocrResult.data.text,
                                    modifiedAt = System.currentTimeMillis()
                                )

                                when (documentRepository.updateDocument(updatedDocument)) {
                                    is Result.Success -> {
                                        withContext(Dispatchers.Main) {
                                            _uiState.value = DocumentDetailsUiState.Success(updatedDocument)
                                        }
                                    }
                                    is Result.Error -> {
                                        withContext(Dispatchers.Main) {
                                            _uiState.value = DocumentDetailsUiState.Error("Failed to save OCR result")
                                        }
                                    }
                                    is Result.Loading -> {}
                                }
                            }
                            is Result.Error -> {
                                withContext(Dispatchers.Main) {
                                    _uiState.value = DocumentDetailsUiState.Error(
                                        ocrResult.exception.message ?: "OCR processing failed"
                                    )
                                }
                            }
                            is Result.Loading -> {}
                        }
                        it.recycle()
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            _uiState.value = DocumentDetailsUiState.Error("Failed to load image")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DocumentDetailsUiState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }
}

/**
 * UI State for Document Details Screen
 */
sealed class DocumentDetailsUiState {
    data object Loading : DocumentDetailsUiState()
    data class Success(val document: Document) : DocumentDetailsUiState()
    data class Error(val message: String) : DocumentDetailsUiState()
}
