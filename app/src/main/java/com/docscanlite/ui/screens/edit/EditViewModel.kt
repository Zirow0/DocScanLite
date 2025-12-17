package com.docscanlite.ui.screens.edit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.FileUtils
import com.docscanlite.core.utils.ImageUtils
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import com.docscanlite.domain.usecase.SaveDocumentUseCase
import com.docscanlite.imageprocessing.ImageProcessor
import com.docscanlite.imageprocessing.transform.PerspectiveTransform
import com.docscanlite.ui.screens.edit.adjust.AdjustViewModel
import com.docscanlite.ui.screens.edit.bounds.BoundsViewModel
import com.docscanlite.ui.screens.edit.crop.CropViewModel
import com.docscanlite.ui.screens.edit.filter.FilterViewModel
import com.docscanlite.ui.screens.edit.rotate.RotateViewModel
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
 * Main Coordinator ViewModel for Edit Screen
 * Orchestrates sub-ViewModels for each tab and manages document loading/saving
 */
@HiltViewModel
class EditViewModel @Inject constructor(
    application: Application,
    private val perspectiveTransform: PerspectiveTransform,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val documentRepository: DocumentRepository
) : AndroidViewModel(application) {

    // Create sub-ViewModels manually
    private val boundsViewModel = BoundsViewModel(application, perspectiveTransform)
    private val filterViewModel = FilterViewModel()
    private val adjustViewModel = AdjustViewModel()
    private val rotateViewModel = RotateViewModel(application)
    private val cropViewModel = CropViewModel()

    private var currentDocumentId: String? = null

    // Path to the true original file (never overwritten)
    private var savedOriginalPath: String? = null

    private val _uiState = MutableStateFlow<EditUiState>(EditUiState.Loading)
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private val _originalImagePath = MutableStateFlow<String?>(null)
    val originalImagePath: StateFlow<String?> = _originalImagePath.asStateFlow()

    private val _croppedImagePath = MutableStateFlow<String?>(null)
    val croppedImagePath: StateFlow<String?> = _croppedImagePath.asStateFlow()

    private val _imageWidth = MutableStateFlow(0)
    val imageWidth: StateFlow<Int> = _imageWidth.asStateFlow()

    private val _imageHeight = MutableStateFlow(0)
    val imageHeight: StateFlow<Int> = _imageHeight.asStateFlow()

    private val _documentName = MutableStateFlow<String?>(null)
    val documentName: StateFlow<String?> = _documentName.asStateFlow()

    // Cached preview bitmaps (loaded once, reused across tabs)
    // Original bitmap - used in Bounds tab for editing bounds
    private val _originalPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val originalPreviewBitmap: StateFlow<Bitmap?> = _originalPreviewBitmap.asStateFlow()

    // Cropped bitmap - used in other tabs after bounds applied
    private val _croppedPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val croppedPreviewBitmap: StateFlow<Bitmap?> = _croppedPreviewBitmap.asStateFlow()

    // Expose sub-ViewModels
    fun getBoundsViewModel() = boundsViewModel
    fun getFilterViewModel() = filterViewModel
    fun getAdjustViewModel() = adjustViewModel
    fun getRotateViewModel() = rotateViewModel
    fun getCropViewModel() = cropViewModel

    /**
     * Load existing document by ID
     */
    fun loadDocumentById(documentId: String) {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading

            when (val result = documentRepository.getDocumentById(documentId)) {
                is Result.Success -> {
                    withContext(Dispatchers.IO) {
                        val document = result.data
                        currentDocumentId = document.id

                        // Check if original file exists
                        val originalFile = File(document.originalPath)
                        val originalExists = originalFile.exists()

                        // Use original path for bounds editing if it exists
                        if (originalExists) {
                            _originalImagePath.value = document.originalPath
                            savedOriginalPath = document.originalPath
                        } else {
                            _originalImagePath.value = document.processedPath
                            savedOriginalPath = document.processedPath
                        }
                        _croppedImagePath.value = document.processedPath

                        // Load document name
                        withContext(Dispatchers.Main) {
                            _documentName.value = document.name
                        }

                        // Load image dimensions from file without loading full bitmap
                        val imageToLoad = if (originalExists) originalFile else File(document.processedPath ?: "")
                        val (width, height) = ImageUtils.getOrientedImageDimensions(imageToLoad)

                        withContext(Dispatchers.Main) {
                            _imageWidth.value = width
                            _imageHeight.value = height
                        }

                        // Load original preview bitmap with EXIF orientation applied (only if original exists)
                        if (originalExists) {
                            val originalBmp = ImageUtils.loadBitmapFromFile(originalFile, maxSize = 4096)
                            withContext(Dispatchers.Main) {
                                _originalPreviewBitmap.value = originalBmp
                            }
                        }

                        // Load cropped/processed preview bitmap if it exists
                        if (document.processedPath != null) {
                            val processedFile = File(document.processedPath!!)
                            if (processedFile.exists()) {
                                val croppedBmp = ImageUtils.loadBitmapFromFile(processedFile, maxSize = 4096)
                                withContext(Dispatchers.Main) {
                                    _croppedPreviewBitmap.value = croppedBmp
                                    // If no original, use processed as original for bounds editing
                                    if (!originalExists) {
                                        _originalPreviewBitmap.value = croppedBmp
                                    }
                                }
                            }
                        }

                        // Load bounds
                        val bounds = document.bounds
                        if (bounds != null && bounds.size == 8) {
                            val corners = listOf(
                                PointF(bounds[0], bounds[1]),
                                PointF(bounds[2], bounds[3]),
                                PointF(bounds[4], bounds[5]),
                                PointF(bounds[6], bounds[7])
                            )
                            boundsViewModel.setCorners(corners)
                            boundsViewModel.setBoundsApplied(true)
                            boundsViewModel.setImageDimensions(width, height)
                        } else {
                            // Set default bounds if not saved (manual mode)
                            val bitmap = ImageUtils.loadBitmapFromFile(imageToLoad, maxSize = 2048)
                            if (bitmap != null) {
                                val defaultCorners = getDefaultCorners(bitmap.width, bitmap.height)
                                boundsViewModel.setCorners(defaultCorners)
                                boundsViewModel.setImageDimensions(bitmap.width, bitmap.height)
                                bitmap.recycle()
                            }
                        }

                        // Load filter settings
                        withContext(Dispatchers.Main) {
                            document.filterName?.let { name ->
                                FilterOption.entries.find { it.name == name }
                            }?.let { filterViewModel.setFilterFromDatabase(it) }

                            // Load adjustments
                            adjustViewModel.setAdjustmentsFromDatabase(
                                document.brightness,
                                document.contrast,
                                document.saturation
                            )

                            // Load crop bounds
                            cropViewModel.setCropBoundsFromDatabase(document.cropBounds)

                            // Load rotation
                            rotateViewModel.setRotationAngleFromDatabase(document.rotationAngle)

                            _uiState.value = EditUiState.Ready

                            // Generate preview if settings were applied
                            if (filterViewModel.selectedFilter.value != FilterOption.NONE ||
                                adjustViewModel.brightness.value != 0f ||
                                adjustViewModel.contrast.value != 0f ||
                                adjustViewModel.saturation.value != 1f) {
                                generatePreview()
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.value = EditUiState.Error(
                        result.exception.message ?: "Не вдалося завантажити документ"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Load image from path (from camera)
     */
    fun loadImageFromPath(imagePath: String) {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading

            try {
                withContext(Dispatchers.IO) {
                    val imageFile = File(imagePath)

                    if (!imageFile.exists()) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = EditUiState.Error("Файл не знайдено: $imagePath")
                        }
                        return@withContext
                    }

                    // Get real image dimensions with EXIF orientation applied
                    val (width, height) = ImageUtils.getOrientedImageDimensions(imageFile)

                    withContext(Dispatchers.Main) {
                        _originalImagePath.value = imagePath
                        savedOriginalPath = imagePath
                        _imageWidth.value = width
                        _imageHeight.value = height
                    }

                    // Load original preview bitmap with EXIF orientation applied
                    val originalBmp = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)
                    withContext(Dispatchers.Main) {
                        _originalPreviewBitmap.value = originalBmp
                        // No cropped image yet
                        _croppedPreviewBitmap.value = null
                    }

                    // Load bitmap for setting default bounds (manual mode)
                    val detectionBitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 2048)
                    if (detectionBitmap != null) {
                        // Set default bounds and dimensions
                        boundsViewModel.setImageDimensions(detectionBitmap.width, detectionBitmap.height)
                        val defaultCorners = getDefaultCorners(detectionBitmap.width, detectionBitmap.height)
                        boundsViewModel.setCorners(defaultCorners)

                        detectionBitmap.recycle()

                        withContext(Dispatchers.Main) {
                            _uiState.value = EditUiState.Ready
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _uiState.value = EditUiState.Error("Не вдалося завантажити зображення")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = EditUiState.Error(e.message ?: "Невідома помилка")
            }
        }
    }

    /**
     * Apply bounds transformation
     */
    fun applyBounds() {
        val imagePath = _originalImagePath.value ?: return

        viewModelScope.launch {
            _uiState.value = EditUiState.ProcessingBounds

            val croppedPath = boundsViewModel.applyBoundsTransform(imagePath)

            if (croppedPath != null) {
                _croppedImagePath.value = croppedPath

                // Reload cropped preview bitmap and dimensions after bounds transform
                withContext(Dispatchers.IO) {
                    val imageFile = File(croppedPath)
                    val (width, height) = ImageUtils.getOrientedImageDimensions(imageFile)
                    val croppedBmp = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)

                    withContext(Dispatchers.Main) {
                        _imageWidth.value = width
                        _imageHeight.value = height
                        _croppedPreviewBitmap.value?.recycle()
                        _croppedPreviewBitmap.value = croppedBmp
                    }
                }

                _uiState.value = EditUiState.Ready
            } else {
                _uiState.value = EditUiState.Error("Не вдалося трансформувати зображення")
            }
        }
    }

    /**
     * Apply crop transformation
     */
    fun applyCrop() {
        val cropBounds = cropViewModel.cropState.value.bounds ?: return
        val sourcePath = _croppedImagePath.value ?: _originalImagePath.value ?: return

        // Only apply if crop mode is not NONE
        if (cropViewModel.cropState.value.mode == CropMode.NONE) {
            android.util.Log.d("EditViewModel", "applyCrop: mode is NONE, skipping")
            return
        }

        android.util.Log.d("EditViewModel", "applyCrop: bounds=$cropBounds, mode=${cropViewModel.cropState.value.mode}")

        viewModelScope.launch {
            _uiState.value = EditUiState.ProcessingBounds

            try {
                withContext(Dispatchers.IO) {
                    // Load source image
                    val sourceFile = File(sourcePath)
                    val sourceBitmap = ImageUtils.loadBitmapFromFile(sourceFile, maxSize = 4096)
                        ?: return@withContext

                    android.util.Log.d("EditViewModel", "applyCrop: loaded bitmap ${sourceBitmap.width}x${sourceBitmap.height}")

                    // Apply crop using ImageProcessor
                    val croppedBitmap = ImageProcessor.cropAndProcess(
                        sourceBitmap,
                        cropBounds.left,
                        cropBounds.top,
                        cropBounds.right,
                        cropBounds.bottom,
                        ImageProcessor.ProcessingOptions() // No additional processing, just crop
                    )

                    android.util.Log.d("EditViewModel", "applyCrop: cropped to ${croppedBitmap.width}x${croppedBitmap.height}")

                    // Save cropped image
                    val croppedFile = FileUtils.createDocumentImageFile(getApplication())
                    ImageUtils.saveBitmap(croppedBitmap, croppedFile)

                    // Update paths and dimensions
                    withContext(Dispatchers.Main) {
                        _croppedImagePath.value = croppedFile.absolutePath
                        _imageWidth.value = croppedBitmap.width
                        _imageHeight.value = croppedBitmap.height

                        // Update preview bitmap
                        _croppedPreviewBitmap.value?.recycle()
                        _croppedPreviewBitmap.value = croppedBitmap

                        android.util.Log.d("EditViewModel", "applyCrop: completed, new path=${croppedFile.absolutePath}")

                        _uiState.value = EditUiState.Ready
                    }

                    sourceBitmap.recycle()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditViewModel", "applyCrop failed", e)
                _uiState.value = EditUiState.Error("Не вдалося обрізати зображення: ${e.message}")
            }
        }
    }

    /**
     * Rotate image
     */
    fun rotateImage(degrees: Float) {
        val sourcePath = _croppedImagePath.value ?: _originalImagePath.value ?: return

        viewModelScope.launch {
            val result = rotateViewModel.rotateImage(sourcePath, degrees)
            if (result != null) {
                _croppedImagePath.value = result.newPath
                _imageWidth.value = result.newWidth
                _imageHeight.value = result.newHeight

                // Reload cropped preview bitmap after rotation
                withContext(Dispatchers.IO) {
                    val croppedBmp = ImageUtils.loadBitmapFromFile(File(result.newPath), maxSize = 4096)
                    withContext(Dispatchers.Main) {
                        _croppedPreviewBitmap.value?.recycle()
                        _croppedPreviewBitmap.value = croppedBmp
                    }
                }

                // Regenerate preview if needed
                generatePreview()
            }
        }
    }

    /**
     * Generate preview with current filter and adjustments
     */
    fun generatePreview() {
        val imagePath = _croppedImagePath.value ?: _originalImagePath.value
        filterViewModel.generatePreview(
            imagePath,
            adjustViewModel.brightness.value,
            adjustViewModel.contrast.value,
            adjustViewModel.saturation.value
        )
    }

    /**
     * Save the final document
     */
    fun saveDocument() {
        viewModelScope.launch {
            // Auto-apply crop if not yet applied and crop mode is active
            if (cropViewModel.cropState.value.mode != CropMode.NONE &&
                cropViewModel.cropState.value.bounds != null) {
                applyCropAndSave()
                return@launch
            }

            // Auto-apply bounds if not yet applied and corners are available
            if (!boundsViewModel.boundsApplied.value && boundsViewModel.corners.value.size == 4) {
                applyBoundsAndSave()
                return@launch
            }

            performSave()
        }
    }

    /**
     * Apply crop first, then save
     */
    private fun applyCropAndSave() {
        val cropBounds = cropViewModel.cropState.value.bounds ?: return
        val sourcePath = _croppedImagePath.value ?: _originalImagePath.value ?: return

        if (cropViewModel.cropState.value.mode == CropMode.NONE) {
            performSave()
            return
        }

        viewModelScope.launch {
            _uiState.value = EditUiState.ProcessingBounds

            try {
                withContext(Dispatchers.IO) {
                    val sourceFile = File(sourcePath)
                    val sourceBitmap = ImageUtils.loadBitmapFromFile(sourceFile, maxSize = 4096)
                        ?: return@withContext

                    val croppedBitmap = ImageProcessor.cropAndProcess(
                        sourceBitmap,
                        cropBounds.left,
                        cropBounds.top,
                        cropBounds.right,
                        cropBounds.bottom,
                        ImageProcessor.ProcessingOptions()
                    )

                    sourceBitmap.recycle()

                    val croppedFile = FileUtils.createDocumentImageFile(getApplication())
                    ImageUtils.saveBitmap(croppedBitmap, croppedFile)

                    _croppedImagePath.value = croppedFile.absolutePath
                    _imageWidth.value = croppedBitmap.width
                    _imageHeight.value = croppedBitmap.height

                    // Update cropped preview bitmap
                    withContext(Dispatchers.Main) {
                        _croppedPreviewBitmap.value?.recycle()
                        _croppedPreviewBitmap.value = croppedBitmap
                    }

                    // Reset crop state
                    cropViewModel.setCropPreset(CropPreset.NONE, 0, 0)

                    _uiState.value = EditUiState.Ready

                    // Now perform save
                    withContext(Dispatchers.Main) {
                        performSave()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditViewModel", "Failed to apply crop and save", e)
                _uiState.value = EditUiState.Error("Не вдалося обрізати зображення: ${e.message}")
            }
        }
    }

    /**
     * Apply bounds first, then save
     */
    private fun applyBoundsAndSave() {
        val imagePath = _originalImagePath.value ?: return

        viewModelScope.launch {
            _uiState.value = EditUiState.ProcessingBounds

            val croppedPath = boundsViewModel.applyBoundsTransform(imagePath)

            if (croppedPath != null) {
                _croppedImagePath.value = croppedPath
                performSave()
            } else {
                _uiState.value = EditUiState.Error("Не вдалося трансформувати зображення")
            }
        }
    }

    /**
     * Perform the actual save operation
     */
    private fun performSave() {
        viewModelScope.launch {
            _uiState.value = EditUiState.Saving

            try {
                withContext(Dispatchers.IO) {
                    val processedImagePath = _croppedImagePath.value ?: _originalImagePath.value
                    if (processedImagePath == null) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = EditUiState.Error("Немає зображення для збереження")
                        }
                        return@withContext
                    }

                    // Check if we need to apply filters/adjustments
                    val needsProcessing = filterViewModel.selectedFilter.value != FilterOption.NONE ||
                            adjustViewModel.brightness.value != 0f ||
                            adjustViewModel.contrast.value != 0f ||
                            adjustViewModel.saturation.value != 1f

                    var bitmap: Bitmap

                    if (needsProcessing) {
                        // Reload in high res and reprocess
                        val imageFile = File(processedImagePath)
                        val highResBitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)
                            ?: return@withContext

                        val options = ImageProcessor.ProcessingOptions(
                            filter = mapFilterOption(filterViewModel.selectedFilter.value),
                            brightness = adjustViewModel.brightness.value,
                            contrast = adjustViewModel.contrast.value,
                            saturation = adjustViewModel.saturation.value
                        )
                        val processedBitmap = ImageProcessor.processBitmap(highResBitmap, options)
                        if (processedBitmap != highResBitmap) {
                            highResBitmap.recycle()
                        }
                        bitmap = processedBitmap
                    } else {
                        val imageFile = File(processedImagePath)
                        bitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)
                            ?: return@withContext
                    }

                    // Use saved original path
                    val permanentOriginalPath = savedOriginalPath ?: _originalImagePath.value ?: processedImagePath

                    // Save processed image
                    val processedFile = FileUtils.createDocumentImageFile(getApplication())
                    ImageUtils.saveBitmap(bitmap, processedFile)

                    // Create thumbnail
                    val thumbnailFile = FileUtils.createThumbnailFile(
                        getApplication(),
                        System.currentTimeMillis().toString()
                    )
                    ImageUtils.createThumbnail(processedFile, thumbnailFile)

                    // Create document with all settings
                    val document = Document(
                        id = currentDocumentId ?: java.util.UUID.randomUUID().toString(),
                        name = _documentName.value?.takeIf { it.isNotBlank() }
                            ?: "Document_${System.currentTimeMillis()}",
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        originalPath = permanentOriginalPath,
                        processedPath = processedFile.absolutePath,
                        thumbnailPath = thumbnailFile.absolutePath,
                        ocrText = null,
                        fileSize = processedFile.length(),
                        width = bitmap.width,
                        height = bitmap.height,
                        tags = emptyList(),
                        bounds = boundsViewModel.getCornersAsList(),
                        filterName = filterViewModel.selectedFilter.value.name,
                        brightness = adjustViewModel.brightness.value,
                        contrast = adjustViewModel.contrast.value,
                        saturation = adjustViewModel.saturation.value,
                        cropBounds = cropViewModel.cropBounds.value,
                        rotationAngle = rotateViewModel.rotationAngle.value
                    )

                    bitmap.recycle()

                    // Save to repository
                    when (val result = saveDocumentUseCase(document)) {
                        is Result.Success -> {
                            withContext(Dispatchers.Main) {
                                _uiState.value = EditUiState.Saved(document.id)
                            }
                        }
                        is Result.Error -> {
                            withContext(Dispatchers.Main) {
                                _uiState.value = EditUiState.Error(
                                    result.exception.message ?: "Не вдалося зберегти документ"
                                )
                            }
                        }
                        is Result.Loading -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = EditUiState.Error(e.message ?: "Невідома помилка")
            }
        }
    }

    /**
     * Set document name
     */
    fun setDocumentName(name: String) {
        _documentName.value = name.trim()
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
            FilterOption.DOCUMENT -> ImageProcessor.FilterType.AUTO_ENHANCE
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

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        filterViewModel.clearPreview()
        _originalPreviewBitmap.value?.recycle()
        _originalPreviewBitmap.value = null
        _croppedPreviewBitmap.value?.recycle()
        _croppedPreviewBitmap.value = null
    }
}
