package com.docscanlite.ui.screens.camera

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.FileUtils
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
 * ViewModel for Camera Screen
 * Handles photo capture and navigation to bounds editing
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Ready)
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _photoUri = MutableStateFlow<Uri?>(null)
    val photoUri: StateFlow<Uri?> = _photoUri.asStateFlow()

    /**
     * Handle photo capture result
     * Saves temporary file and navigates to bounds editing
     */
    fun onPhotoCaptured(tempImageFile: File) {
        viewModelScope.launch {
            _uiState.value = CameraUiState.Processing

            try {
                withContext(Dispatchers.IO) {
                    // Create permanent image file
                    val permanentFile = FileUtils.createDocumentImageFile(getApplication())

                    // Copy temp file to permanent location
                    tempImageFile.copyTo(permanentFile, overwrite = true)

                    // Delete temp file
                    FileUtils.deleteFile(tempImageFile)

                    withContext(Dispatchers.Main) {
                        // Navigate to bounds editing screen
                        _uiState.value = CameraUiState.PhotoCaptured(permanentFile.absolutePath)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = CameraUiState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Reset state to ready
     */
    fun resetState() {
        _uiState.value = CameraUiState.Ready
    }

    /**
     * Set photo URI for preview
     */
    fun setPhotoUri(uri: Uri?) {
        _photoUri.value = uri
    }
}

/**
 * UI State for Camera Screen
 */
sealed class CameraUiState {
    data object Ready : CameraUiState()
    data object Processing : CameraUiState()
    data class PhotoCaptured(val imagePath: String) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}
