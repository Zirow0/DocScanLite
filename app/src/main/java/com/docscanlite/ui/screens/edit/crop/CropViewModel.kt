package com.docscanlite.ui.screens.edit.crop

import android.graphics.RectF
import androidx.lifecycle.ViewModel
import com.docscanlite.ui.screens.edit.CropMode
import com.docscanlite.ui.screens.edit.CropPreset
import com.docscanlite.ui.screens.edit.CropState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Crop Tab
 * Manages crop presets and bounds
 */
class CropViewModel : ViewModel() {

    private val _cropState = MutableStateFlow(CropState())
    val cropState: StateFlow<CropState> = _cropState.asStateFlow()

    private val _cropBounds = MutableStateFlow<List<Float>?>(null)
    val cropBounds: StateFlow<List<Float>?> = _cropBounds.asStateFlow()

    /**
     * Set crop bounds from external source (e.g. loaded from database)
     */
    fun setCropBoundsFromDatabase(bounds: List<Float>?) {
        _cropBounds.value = bounds
    }

    /**
     * Set crop preset (NONE, A4, FREE, A5, 16:9, 3:4)
     * @param preset The crop preset to apply
     * @param imageWidth Width of the current preview bitmap
     * @param imageHeight Height of the current preview bitmap
     */
    fun setCropPreset(preset: CropPreset, imageWidth: Int, imageHeight: Int) {
        android.util.Log.d("CropViewModel", "=== setCropPreset ===")
        android.util.Log.d("CropViewModel", "preset=$preset, imageSize=${imageWidth}x${imageHeight}")

        val currentState = _cropState.value
        android.util.Log.d("CropViewModel", "currentState: mode=${currentState.mode}, preset=${currentState.preset}, isVertical=${currentState.isVertical}")

        // Determine new mode
        val newMode = when (preset) {
            CropPreset.NONE -> CropMode.NONE
            CropPreset.FREE -> CropMode.FREE
            else -> CropMode.PRESET
        }
        android.util.Log.d("CropViewModel", "newMode=$newMode")

        // If clicking same preset with flip capability - flip orientation
        if (currentState.preset == preset && preset.canFlip && newMode == CropMode.PRESET) {
            val newIsVertical = !currentState.isVertical
            val newBounds = calculatePresetBounds(preset, newIsVertical, imageWidth, imageHeight)
            android.util.Log.d("CropViewModel", "FLIP: preset=$preset, isVertical=$newIsVertical, bounds=$newBounds")
            _cropState.value = currentState.copy(
                mode = newMode,
                isVertical = newIsVertical,
                bounds = newBounds
            )
            return
        }

        // For FREE/NONE modes, preserve current bounds and orientation
        // For PRESET modes, check if we should start with current orientation or default vertical
        val newIsVertical = when {
            preset == CropPreset.FREE || preset == CropPreset.NONE -> currentState.isVertical
            preset.canFlip && currentState.mode == CropMode.PRESET -> currentState.isVertical
            else -> true
        }

        val newBounds = when (preset) {
            CropPreset.NONE -> null
            CropPreset.FREE -> currentState.bounds ?: RectF(0f, 0f, 1f, 1f)
            else -> calculatePresetBounds(preset, newIsVertical, imageWidth, imageHeight)
        }

        android.util.Log.d("CropViewModel", "SET_PRESET: preset=$preset, mode=$newMode, isVertical=$newIsVertical, bounds=$newBounds")

        _cropState.value = currentState.copy(
            mode = newMode,
            preset = preset,
            isVertical = newIsVertical,
            bounds = newBounds
        )
    }

    /**
     * Update crop bounds (for interactive dragging)
     */
    fun updateCropBounds(newBounds: RectF) {
        val currentState = _cropState.value
        val width = newBounds.width()
        val height = newBounds.height()
        val ratio = width / height

        android.util.Log.d("CropViewModel", "updateCropBounds: bounds=$newBounds")
        android.util.Log.d("CropViewModel", "  width=$width, height=$height, ratio=$ratio")
        android.util.Log.d("CropViewModel", "  current mode=${currentState.mode}, preset=${currentState.preset}, isVertical=${currentState.isVertical}")

        // Keep isVertical unchanged during dragging
        // User can explicitly flip orientation using preset button
        _cropState.value = currentState.copy(
            bounds = newBounds
        )
    }

    /**
     * Calculate bounds for preset aspect ratio
     * Creates maximum size crop frame with given aspect ratio that fits the image
     * @param preset The crop preset
     * @param isVertical Whether to use vertical orientation
     * @param imageWidth Width of the preview bitmap
     * @param imageHeight Height of the preview bitmap
     */
    private fun calculatePresetBounds(preset: CropPreset, isVertical: Boolean, imageWidth: Int, imageHeight: Int): RectF {
        val aspectRatio = preset.getAspectRatio(isVertical) ?: return RectF(0f, 0f, 1f, 1f)

        // Get image dimensions
        val imgWidth = imageWidth.toFloat()
        val imgHeight = imageHeight.toFloat()

        if (imgWidth <= 0 || imgHeight <= 0) {
            // Fallback if image not loaded yet
            return RectF(0f, 0f, 1f, 1f)
        }

        val imageAspect = imgWidth / imgHeight

        // Calculate crop dimensions to fit within image bounds
        // aspectRatio = width / height of desired crop
        val cropWidth: Float
        val cropHeight: Float

        if (aspectRatio > imageAspect) {
            // Crop is wider relative to image - width is limiting dimension
            cropWidth = 1f
            cropHeight = 1f / (aspectRatio / imageAspect)
        } else {
            // Crop is taller relative to image - height is limiting dimension
            cropHeight = 1f
            cropWidth = aspectRatio / imageAspect
        }

        // Center the crop frame
        val left = (1f - cropWidth) / 2f
        val top = (1f - cropHeight) / 2f

        val result = RectF(left, top, left + cropWidth, top + cropHeight)
        android.util.Log.d("CropViewModel", "calculatePresetBounds: preset=$preset, isVertical=$isVertical, aspectRatio=$aspectRatio, imageAspect=$imageAspect, cropWidth=$cropWidth, cropHeight=$cropHeight, result=$result")
        return result
    }

    /**
     * Set crop bounds directly (normalized 0-1)
     */
    fun setCropBounds(bounds: List<Float>?) {
        _cropBounds.value = bounds
    }
}
