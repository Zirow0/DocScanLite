package com.docscanlite.ui.screens.edit.filter

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.ImageUtils
import com.docscanlite.imageprocessing.ImageProcessor
import com.docscanlite.ui.screens.edit.FilterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for Filter Tab
 * Manages filter selection and preview generation
 */
class FilterViewModel : ViewModel() {

    private val _selectedFilter = MutableStateFlow(FilterOption.NONE)
    val selectedFilter: StateFlow<FilterOption> = _selectedFilter.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    // Job for preview generation (to cancel previous jobs)
    private var previewJob: Job? = null

    // Flag to track if preview is currently being processed
    private var isPreviewProcessing = false

    // Last preview update timestamp (for 60 FPS throttling)
    private var lastPreviewTime = 0L

    /**
     * Set filter from external source (e.g. loaded from database)
     */
    fun setFilterFromDatabase(filter: FilterOption) {
        _selectedFilter.value = filter
    }

    /**
     * Update filter selection and generate preview
     */
    fun setFilter(
        filter: FilterOption,
        imagePath: String?,
        brightness: Float = 0f,
        contrast: Float = 0f,
        saturation: Float = 1f
    ) {
        _selectedFilter.value = filter
        generatePreview(imagePath, brightness, contrast, saturation)
    }

    /**
     * Generate preview with current filter and adjustments
     */
    fun generatePreview(
        imagePath: String?,
        brightness: Float = 0f,
        contrast: Float = 0f,
        saturation: Float = 1f
    ) {
        // Skip if preview is currently being processed
        if (isPreviewProcessing) {
            return
        }

        // 60 FPS throttling: minimum 16ms between updates
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastPreviewTime
        if (timeSinceLastUpdate < 16) {
            return
        }

        // Cancel previous preview job
        previewJob?.cancel()

        if (imagePath == null) return

        // Check if any processing is needed
        val needsProcessing = _selectedFilter.value != FilterOption.NONE ||
                brightness != 0f ||
                contrast != 0f ||
                saturation != 1f

        if (!needsProcessing) {
            clearPreview()
            return
        }

        previewJob = viewModelScope.launch {
            // Mark as processing
            isPreviewProcessing = true
            lastPreviewTime = System.currentTimeMillis()

            try {
                withContext(Dispatchers.IO) {
                    val imageFile = File(imagePath)
                    val bitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 1024) // Lower res for preview

                    if (bitmap == null) return@withContext

                    // Build processing options
                    val options = ImageProcessor.ProcessingOptions(
                        filter = mapFilterOption(_selectedFilter.value),
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation
                    )

                    // Apply processing
                    val processedBitmap = ImageProcessor.processBitmap(bitmap, options)

                    // Update preview bitmap on Main thread
                    withContext(Dispatchers.Main) {
                        // Recycle old preview bitmap if exists
                        _previewBitmap.value?.recycle()

                        // Set new preview bitmap (keep in RAM, don't save to file)
                        _previewBitmap.value = processedBitmap
                    }

                    // Clean up source bitmap if different from processed
                    if (processedBitmap != bitmap) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Clear preview on error
                    _previewBitmap.value?.recycle()
                    _previewBitmap.value = null
                }
            } finally {
                // Mark processing as complete
                isPreviewProcessing = false
            }
        }
    }

    /**
     * Clear preview bitmap
     */
    fun clearPreview() {
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
    }

    /**
     * Map FilterOption to ImageProcessor.FilterType
     */
    private fun mapFilterOption(option: FilterOption): ImageProcessor.FilterType {
        return when (option) {
            FilterOption.NONE -> ImageProcessor.FilterType.NONE
            FilterOption.AUTO_ENHANCE -> ImageProcessor.FilterType.AUTO_ENHANCE
            FilterOption.BLACK_AND_WHITE -> ImageProcessor.FilterType.BLACK_AND_WHITE
            FilterOption.GRAYSCALE -> ImageProcessor.FilterType.GRAYSCALE
            FilterOption.SEPIA -> ImageProcessor.FilterType.SEPIA
            FilterOption.DOCUMENT -> ImageProcessor.FilterType.AUTO_ENHANCE // Use auto enhance for document
        }
    }

    /**
     * Clean up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        // Recycle preview bitmap to free memory
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
    }
}
