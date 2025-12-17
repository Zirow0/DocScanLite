package com.docscanlite.ui.screens.edit.adjust

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Adjust Tab
 * Manages brightness, contrast, and saturation adjustments
 */
class AdjustViewModel : ViewModel() {

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(0f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(1f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    // Saved values from DB (for double-tap reset)
    private val _savedBrightness = MutableStateFlow(0f)
    val savedBrightness: StateFlow<Float> = _savedBrightness.asStateFlow()

    private val _savedContrast = MutableStateFlow(0f)
    val savedContrast: StateFlow<Float> = _savedContrast.asStateFlow()

    private val _savedSaturation = MutableStateFlow(1f)
    val savedSaturation: StateFlow<Float> = _savedSaturation.asStateFlow()

    /**
     * Set adjustments from external source (e.g. loaded from database)
     */
    fun setAdjustmentsFromDatabase(brightness: Float, contrast: Float, saturation: Float) {
        _brightness.value = brightness
        _contrast.value = contrast
        _saturation.value = saturation
        _savedBrightness.value = brightness
        _savedContrast.value = contrast
        _savedSaturation.value = saturation
    }

    /**
     * Update brightness and trigger preview generation externally
     */
    fun setBrightness(value: Float) {
        _brightness.value = value
    }

    /**
     * Update contrast and trigger preview generation externally
     */
    fun setContrast(value: Float) {
        _contrast.value = value
    }

    /**
     * Update saturation and trigger preview generation externally
     */
    fun setSaturation(value: Float) {
        _saturation.value = value
    }

    /**
     * Reset adjustments to default
     */
    fun resetAdjustments() {
        _brightness.value = 0f
        _contrast.value = 0f
        _saturation.value = 1f
    }

    /**
     * Check if any adjustments are applied
     */
    fun hasAdjustments(): Boolean {
        return _brightness.value != 0f || _contrast.value != 0f || _saturation.value != 1f
    }
}
