package com.docscanlite.domain.model

/**
 * Image processing settings
 */
data class ProcessingSettings(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val filter: FilterType = FilterType.NONE,
    val rotation: Int = 0
)

enum class FilterType {
    NONE,
    AUTO,
    BLACK_AND_WHITE,
    GRAYSCALE,
    COLOR,
    HIGH_CONTRAST
}
