package com.docscanlite.ui.screens.edit

import android.graphics.RectF

/**
 * UI State for Edit Screen
 */
sealed class EditUiState {
    data object Loading : EditUiState()
    data object Ready : EditUiState()
    data object ProcessingBounds : EditUiState()
    data object Saving : EditUiState()
    data class Saved(val documentId: String) : EditUiState()
    data class Error(val message: String) : EditUiState()
}

/**
 * Filter options for image processing
 */
enum class FilterOption(val displayName: String) {
    NONE("Оригінал"),
    AUTO_ENHANCE("Авто"),
    BLACK_AND_WHITE("Ч/Б"),
    GRAYSCALE("Сірий"),
    SEPIA("Сепія"),
    DOCUMENT("Документ")
}

/**
 * Crop mode
 */
enum class CropMode {
    NONE,           // No cropping
    FREE,           // Free cropping
    PRESET          // Preset aspect ratio
}

/**
 * Crop preset
 */
enum class CropPreset(
    val displayName: String,
    val aspectRatio: Float?,
    val canFlip: Boolean
) {
    NONE("Без змін", null, false),
    A4("A4", 210f / 297f, true),
    FREE("Вільне", null, false),
    A5("A5", 148f / 210f, true),
    RATIO_16_9("16:9", 16f / 9f, true),
    RATIO_3_4("3:4", 3f / 4f, true);

    fun getDisplayNameWithOrientation(isVertical: Boolean): String {
        return when {
            !canFlip -> displayName
            isVertical -> "$displayName ↕"
            else -> "$displayName ↔"
        }
    }

    fun getAspectRatio(isVertical: Boolean): Float? {
        return when {
            aspectRatio == null -> null
            isVertical -> aspectRatio
            else -> 1f / aspectRatio!!
        }
    }
}

/**
 * Crop state data
 */
data class CropState(
    val mode: CropMode = CropMode.NONE,
    val preset: CropPreset = CropPreset.NONE,
    val isVertical: Boolean = true,
    val bounds: RectF? = null  // Normalized 0..1
) {
    fun getCurrentAspectRatio(): Float? {
        return when (mode) {
            CropMode.PRESET -> preset.getAspectRatio(isVertical)
            CropMode.FREE -> bounds?.let { it.width() / it.height() }
            CropMode.NONE -> null
        }
    }

    fun getDisplayText(): String {
        return when (mode) {
            CropMode.NONE -> preset.displayName
            CropMode.PRESET -> preset.getDisplayNameWithOrientation(isVertical)
            CropMode.FREE -> {
                val ratio = getCurrentAspectRatio()
                if (ratio != null) {
                    String.format("%.2f:1", ratio)
                } else {
                    "Вільне"
                }
            }
        }
    }
}

/**
 * Crop handle types
 */
enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT
}
