package com.docscanlite.ui.screens.edit.components.frames

import androidx.compose.runtime.Immutable

/**
 * Image transformation state for zoom and pan
 * Used to synchronize transformations between image and overlay layers
 */
@Immutable
data class ImageTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    companion object {
        val Default = ImageTransform()

        // Scale limits
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 3f
    }

    /**
     * Apply scale limits
     */
    fun coerceScale(): ImageTransform {
        return copy(scale = scale.coerceIn(MIN_SCALE, MAX_SCALE))
    }
}
