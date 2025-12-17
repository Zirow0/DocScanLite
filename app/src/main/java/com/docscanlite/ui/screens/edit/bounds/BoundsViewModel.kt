package com.docscanlite.ui.screens.edit.bounds

import android.app.Application
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.FileUtils
import com.docscanlite.core.utils.ImageUtils
import com.docscanlite.imageprocessing.transform.PerspectiveTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for Bounds Tab
 * Manages document bounds detection and corner manipulation
 */
class BoundsViewModel(
    application: Application,
    private val perspectiveTransform: PerspectiveTransform
) : AndroidViewModel(application) {

    private val _corners = MutableStateFlow<List<PointF>>(emptyList())
    val corners: StateFlow<List<PointF>> = _corners.asStateFlow()

    private val _originalImageWidth = MutableStateFlow(0)
    val originalImageWidth: StateFlow<Int> = _originalImageWidth.asStateFlow()

    private val _originalImageHeight = MutableStateFlow(0)
    val originalImageHeight: StateFlow<Int> = _originalImageHeight.asStateFlow()

    private val _boundsApplied = MutableStateFlow(false)
    val boundsApplied: StateFlow<Boolean> = _boundsApplied.asStateFlow()

    // Store last applied corners to detect changes
    private var lastAppliedCorners: List<PointF> = emptyList()

    /**
     * Set corners from external source (e.g. loaded from database)
     */
    fun setCorners(corners: List<PointF>) {
        _corners.value = corners
    }

    /**
     * Set image dimensions
     */
    fun setImageDimensions(width: Int, height: Int) {
        _originalImageWidth.value = width
        _originalImageHeight.value = height
    }

    /**
     * Set bounds applied state
     */
    fun setBoundsApplied(applied: Boolean) {
        _boundsApplied.value = applied
        if (applied) {
            lastAppliedCorners = _corners.value.map { PointF(it.x, it.y) }
        }
    }

    /**
     * Update corner position during drag
     */
    fun updateCorner(index: Int, newPosition: PointF) {
        val currentCorners = _corners.value.toMutableList()
        if (index in currentCorners.indices) {
            currentCorners[index] = newPosition
            _corners.value = currentCorners

            // Check if corners changed from last applied - allow re-apply
            if (cornersChanged()) {
                _boundsApplied.value = false
            }
        }
    }

    /**
     * Check if current corners differ from last applied corners
     */
    private fun cornersChanged(): Boolean {
        if (lastAppliedCorners.size != _corners.value.size) return true
        return _corners.value.zip(lastAppliedCorners).any { (current, applied) ->
            kotlin.math.abs(current.x - applied.x) > 1f || kotlin.math.abs(current.y - applied.y) > 1f
        }
    }

    /**
     * Apply bounds transformation and create cropped image
     * Returns the path to the cropped image or null if failed
     */
    suspend fun applyBoundsTransform(imagePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val imageFile = File(imagePath)
                val bitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)
                    ?: return@withContext null

                // Apply perspective transform
                val transformedBitmap = perspectiveTransform.transform(bitmap, _corners.value)
                bitmap.recycle()

                if (transformedBitmap == null) {
                    return@withContext null
                }

                // Save cropped image to temp file
                val croppedFile = FileUtils.createDocumentImageFile(getApplication())
                ImageUtils.saveBitmap(transformedBitmap, croppedFile)

                withContext(Dispatchers.Main) {
                    _boundsApplied.value = true
                    // Save applied corners for change detection
                    lastAppliedCorners = _corners.value.map { PointF(it.x, it.y) }
                }

                transformedBitmap.recycle()

                croppedFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Get corners as flat list for saving to database
     * Returns [x1, y1, x2, y2, x3, y3, x4, y4] or null
     */
    fun getCornersAsList(): List<Float>? {
        return if (_corners.value.size == 4) {
            _corners.value.flatMap { listOf(it.x, it.y) }
        } else {
            null
        }
    }
}
