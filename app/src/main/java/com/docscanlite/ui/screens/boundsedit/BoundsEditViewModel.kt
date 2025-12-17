package com.docscanlite.ui.screens.boundsedit

import android.app.Application
import android.graphics.PointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.FileUtils
import com.docscanlite.core.utils.ImageUtils
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.Document
import com.docscanlite.domain.usecase.SaveDocumentUseCase
import com.docscanlite.imageprocessing.transform.PerspectiveTransform
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
 * ViewModel for Document Bounds Editing Screen
 * Manages document boundary detection and editing
 */
@HiltViewModel
class BoundsEditViewModel @Inject constructor(
    application: Application,
    private val perspectiveTransform: PerspectiveTransform,
    private val saveDocumentUseCase: SaveDocumentUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BoundsEditUiState>(BoundsEditUiState.Loading)
    val uiState: StateFlow<BoundsEditUiState> = _uiState.asStateFlow()

    private val _corners = MutableStateFlow<List<PointF>>(emptyList())
    val corners: StateFlow<List<PointF>> = _corners.asStateFlow()

    /**
     * Load image and set default bounds (manual mode)
     */
    fun loadImage(imagePath: String) {
        viewModelScope.launch {
            _uiState.value = BoundsEditUiState.Loading

            try {
                withContext(Dispatchers.IO) {
                    val imageFile = File(imagePath)
                    val bitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 2048)

                    if (bitmap != null) {
                        // Store image dimensions for bounds checking
                        currentImageWidth = bitmap.width
                        currentImageHeight = bitmap.height

                        // Set default corners with padding (manual mode)
                        val defaultCorners = getDefaultCorners(bitmap.width, bitmap.height)

                        withContext(Dispatchers.Main) {
                            _corners.value = defaultCorners
                            _uiState.value = BoundsEditUiState.Ready(imagePath, bitmap.width, bitmap.height)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _uiState.value = BoundsEditUiState.Error("Failed to load image")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = BoundsEditUiState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Get default corners with padding (5% from edges)
     */
    private fun getDefaultCorners(width: Int, height: Int): List<PointF> {
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 0.05f
        val px = w * padding
        val py = h * padding

        return listOf(
            PointF(px, py),           // top-left
            PointF(w - px, py),       // top-right
            PointF(w - px, h - py),   // bottom-right
            PointF(px, h - py)        // bottom-left
        )
    }

    // Store image dimensions for bounds checking
    private var currentImageWidth: Int = 0
    private var currentImageHeight: Int = 0

    /**
     * Update corner position with bounds checking
     */
    fun updateCorner(index: Int, newPosition: PointF) {
        val currentCorners = _corners.value.toMutableList()
        if (index in currentCorners.indices) {
            // Ensure position is within image bounds with margin
            val minMargin = 10f
            val maxX = (currentImageWidth - minMargin).coerceAtLeast(minMargin)
            val maxY = (currentImageHeight - minMargin).coerceAtLeast(minMargin)

            val clampedPosition = PointF(
                newPosition.x.coerceIn(minMargin, maxX),
                newPosition.y.coerceIn(minMargin, maxY)
            )

            currentCorners[index] = clampedPosition
            _corners.value = currentCorners
        }
    }

    /**
     * Confirm bounds and process document
     */
    fun confirmBounds(imagePath: String) {
        viewModelScope.launch {
            _uiState.value = BoundsEditUiState.Processing

            try {
                withContext(Dispatchers.IO) {
                    val imageFile = File(imagePath)
                    val bitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)

                    if (bitmap == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = BoundsEditUiState.Error("Failed to load image")
                        }
                        return@withContext
                    }

                    // Apply perspective transform
                    val transformedBitmap = perspectiveTransform.transform(bitmap, _corners.value)
                    bitmap.recycle()

                    if (transformedBitmap == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = BoundsEditUiState.Error("Failed to transform image")
                        }
                        return@withContext
                    }

                    // Save processed image
                    val processedFile = FileUtils.createDocumentImageFile(getApplication())
                    ImageUtils.saveBitmap(transformedBitmap, processedFile)

                    // Create thumbnail
                    val thumbnailFile = FileUtils.createThumbnailFile(
                        getApplication(),
                        System.currentTimeMillis().toString()
                    )
                    ImageUtils.createThumbnail(processedFile, thumbnailFile)

                    // Create document
                    val document = Document(
                        name = "Document_${System.currentTimeMillis()}",
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        originalPath = imagePath,
                        processedPath = processedFile.absolutePath,
                        thumbnailPath = thumbnailFile.absolutePath,
                        ocrText = null,
                        fileSize = processedFile.length(),
                        width = transformedBitmap.width,
                        height = transformedBitmap.height,
                        tags = emptyList()
                    )

                    transformedBitmap.recycle()

                    // Save to repository
                    when (val result = saveDocumentUseCase(document)) {
                        is Result.Success -> {
                            withContext(Dispatchers.Main) {
                                _uiState.value = BoundsEditUiState.Success(document.id)
                            }
                        }
                        is Result.Error -> {
                            withContext(Dispatchers.Main) {
                                _uiState.value = BoundsEditUiState.Error(
                                    result.exception.message ?: "Failed to save document"
                                )
                            }
                        }
                        is Result.Loading -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = BoundsEditUiState.Error(
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

}

/**
 * UI State for Bounds Edit Screen
 */
sealed class BoundsEditUiState {
    data object Loading : BoundsEditUiState()
    data class Ready(val imagePath: String, val imageWidth: Int, val imageHeight: Int) : BoundsEditUiState()
    data object Processing : BoundsEditUiState()
    data class Success(val documentId: String) : BoundsEditUiState()
    data class Error(val message: String) : BoundsEditUiState()
}
