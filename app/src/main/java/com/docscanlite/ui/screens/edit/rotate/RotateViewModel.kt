package com.docscanlite.ui.screens.edit.rotate

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docscanlite.core.utils.FileUtils
import com.docscanlite.core.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for Rotate Tab
 * Manages image rotation
 */
class RotateViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _rotationAngle = MutableStateFlow(0f)
    val rotationAngle: StateFlow<Float> = _rotationAngle.asStateFlow()

    /**
     * Set rotation angle from external source (e.g. loaded from database)
     */
    fun setRotationAngleFromDatabase(angle: Float) {
        _rotationAngle.value = angle
    }

    /**
     * Rotate image by specified degrees (adds to current rotation)
     * Only updates the angle value without applying
     */
    fun rotate(degrees: Float) {
        val newAngle = (_rotationAngle.value + degrees) % 360f
        _rotationAngle.value = if (newAngle < 0) newAngle + 360f else newAngle
    }

    /**
     * Rotate the actual image and update cropped path
     * Returns the new image path or null if failed
     */
    suspend fun rotateImage(imagePath: String, degrees: Float): RotateResult? {
        return withContext(Dispatchers.IO) {
            try {
                val imageFile = File(imagePath)
                val bitmap = ImageUtils.loadBitmapFromFile(imageFile, maxSize = 4096)
                    ?: return@withContext null

                // Create rotation matrix
                val matrix = android.graphics.Matrix()
                matrix.postRotate(degrees)

                // Apply rotation
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height,
                    matrix, true
                )

                // Save rotated image
                val rotatedFile = FileUtils.createDocumentImageFile(getApplication())
                ImageUtils.saveBitmap(rotatedBitmap, rotatedFile)

                // Update angle
                val newAngle = (_rotationAngle.value + degrees) % 360f
                withContext(Dispatchers.Main) {
                    _rotationAngle.value = if (newAngle < 0) newAngle + 360f else newAngle
                }

                val result = RotateResult(
                    newPath = rotatedFile.absolutePath,
                    newWidth = rotatedBitmap.width,
                    newHeight = rotatedBitmap.height
                )

                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap.recycle()

                result
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Set rotation angle directly
     */
    fun setRotationAngle(angle: Float) {
        _rotationAngle.value = angle
    }
}

/**
 * Result of rotation operation
 */
data class RotateResult(
    val newPath: String,
    val newWidth: Int,
    val newHeight: Int
)
