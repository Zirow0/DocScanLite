package com.docscanlite.ui.screens.edit.adjust

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.docscanlite.ui.screens.edit.components.TabContentWrapper
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * Adjust Tab Content
 * Shows image with adjustment preview and adjustment panel
 */
@Composable
fun AdjustTabContent(
    imagePath: String?,
    previewBitmap: Bitmap?,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    savedBrightness: Float,
    savedContrast: Float,
    savedSaturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
    onOverlayStateChange: (AdjustmentOverlayState?) -> Unit,
    modifier: Modifier = Modifier
) {
    TabContentWrapper(
        imagePath = imagePath,
        previewBitmap = previewBitmap,
        panelContent = {
            AdjustPanel(
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                savedBrightness = savedBrightness,
                savedContrast = savedContrast,
                savedSaturation = savedSaturation,
                onBrightnessChange = onBrightnessChange,
                onContrastChange = onContrastChange,
                onSaturationChange = onSaturationChange,
                onReset = onReset,
                onOverlayStateChange = onOverlayStateChange
            )
        },
        modifier = modifier
    )
}

/**
 * Compact adjustment panel with vertical drag gesture
 */
@Composable
private fun AdjustPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    savedBrightness: Float,
    savedContrast: Float,
    savedSaturation: Float,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
    onOverlayStateChange: (AdjustmentOverlayState?) -> Unit
) {
    // Saved values from DB (or defaults if not saved)
    val savedValues = remember(savedBrightness, savedContrast, savedSaturation) {
        mapOf(
            AdjustParameter.BRIGHTNESS to savedBrightness,
            AdjustParameter.CONTRAST to savedContrast,
            AdjustParameter.SATURATION to savedSaturation
        )
    }

    // Main compact panel with icons
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            AdjustmentItem(
                icon = Icons.Default.WbSunny,
                label = "Яскравість",
                value = brightness,
                formatValue = { it.toInt().toString() },
                parameter = AdjustParameter.BRIGHTNESS,
                range = -100f..100f,
                defaultValue = 0f,
                savedValue = savedValues[AdjustParameter.BRIGHTNESS] ?: 0f,
                sensitivity = 0.8f,
                onDragStart = { param, value ->
                    onOverlayStateChange(
                        AdjustmentOverlayState(
                            parameter = param,
                            value = value,
                            range = -100f..100f,
                            formatValue = { v -> v.toInt().toString() }
                        )
                    )
                },
                onDragUpdate = { newValue ->
                    onOverlayStateChange(
                        AdjustmentOverlayState(
                            parameter = AdjustParameter.BRIGHTNESS,
                            value = newValue,
                            range = -100f..100f,
                            formatValue = { v -> v.toInt().toString() }
                        )
                    )
                    onBrightnessChange(newValue)
                },
                onDragEnd = {
                    onOverlayStateChange(null)
                },
                onDoubleTap = { savedVal ->
                    onBrightnessChange(savedVal)
                }
            )
        }
        item {
            AdjustmentItem(
                icon = Icons.Default.Contrast,
                label = "Контраст",
                value = contrast,
                formatValue = { String.format("%.2f", it) },
                parameter = AdjustParameter.CONTRAST,
                range = -1f..1f,
                defaultValue = 0f,
                savedValue = savedValues[AdjustParameter.CONTRAST] ?: 0f,
                sensitivity = 0.008f,
                onDragStart = { param, value ->
                    onOverlayStateChange(
                        AdjustmentOverlayState(
                            parameter = param,
                            value = value,
                            range = -1f..1f,
                            formatValue = { v -> String.format("%.2f", v) }
                        )
                    )
                },
                onDragUpdate = { newValue ->
                    onOverlayStateChange(
                        AdjustmentOverlayState(
                            parameter = AdjustParameter.CONTRAST,
                            value = newValue,
                            range = -1f..1f,
                            formatValue = { v -> String.format("%.2f", v) }
                        )
                    )
                    onContrastChange(newValue)
                },
                onDragEnd = {
                    onOverlayStateChange(null)
                },
                onDoubleTap = { savedVal ->
                    onContrastChange(savedVal)
                }
            )
        }
        item {
            AdjustmentItem(
                icon = Icons.Default.WaterDrop,
                label = "Насиченість",
                value = saturation,
                formatValue = { String.format("%.2f", it) },
                parameter = AdjustParameter.SATURATION,
                range = 0f..2f,
                defaultValue = 1f,
                savedValue = savedValues[AdjustParameter.SATURATION] ?: 1f,
                sensitivity = 0.012f,
                onDragStart = { param, value ->
                    onOverlayStateChange(
                        AdjustmentOverlayState(
                            parameter = param,
                            value = value,
                            range = 0f..2f,
                            formatValue = { v -> String.format("%.2f", v) }
                        )
                    )
                },
                onDragUpdate = { newValue ->
                    onOverlayStateChange(
                        AdjustmentOverlayState(
                            parameter = AdjustParameter.SATURATION,
                            value = newValue,
                            range = 0f..2f,
                            formatValue = { v -> String.format("%.2f", v) }
                        )
                    )
                    onSaturationChange(newValue)
                },
                onDragEnd = {
                    onOverlayStateChange(null)
                },
                onDoubleTap = { savedVal ->
                    onSaturationChange(savedVal)
                }
            )
        }
        item {
            // Reset all button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable {
                        onReset()
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Скинути все",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Скинути",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Single adjustment item with vertical drag
 */
@Composable
private fun AdjustmentItem(
    icon: ImageVector,
    label: String,
    value: Float,
    formatValue: (Float) -> String,
    parameter: AdjustParameter,
    range: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    savedValue: Float,
    sensitivity: Float,
    onDragStart: (AdjustParameter, Float) -> Unit,
    onDragUpdate: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDoubleTap: (Float) -> Unit
) {
    var startY by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showOverlayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Value to restore on double tap: saved value if different from default, otherwise default
    val resetValue = if (savedValue != defaultValue) savedValue else defaultValue
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startY = offset.y
                        dragStartValue = value
                        isDragging = true
                        // Don't show overlay on drag start anymore
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val currentY = change.position.y
                        val deltaY = startY - currentY // Inverted: up = positive

                        // Exponential sensitivity based on distance
                        // Higher exponent (1.6) = less sensitive near zero, more sensitive far
                        val absDelta = kotlin.math.abs(deltaY)
                        val sign = if (deltaY >= 0) 1f else -1f
                        val exponentialDelta = absDelta.pow(1.6f) * sensitivity * sign * 0.05f

                        val newValue = (dragStartValue + exponentialDelta).coerceIn(range)
                        onDragUpdate(newValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    }
                )
            }
            .pointerInput(resetValue) {
                detectTapGestures(
                    onPress = {
                        // Cancel any existing overlay show job
                        showOverlayJob?.cancel()

                        // Wait 300ms to avoid conflict with double-tap
                        showOverlayJob = scope.launch {
                            kotlinx.coroutines.delay(300)
                            if (!isDragging) {
                                onDragStart(parameter, value)
                            }
                        }

                        // Wait for release or cancel
                        tryAwaitRelease()
                    },
                    onDoubleTap = {
                        // Cancel overlay show on double-tap
                        showOverlayJob?.cancel()
                        onDoubleTap(resetValue)
                    }
                )
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = if (value != defaultValue)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatValue(value),
            style = MaterialTheme.typography.labelSmall,
            color = if (value != defaultValue)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
