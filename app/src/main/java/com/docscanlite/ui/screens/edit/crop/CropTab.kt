package com.docscanlite.ui.screens.edit.crop

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docscanlite.ui.screens.edit.CropMode
import com.docscanlite.ui.screens.edit.CropPreset
import com.docscanlite.ui.screens.edit.CropState
import com.docscanlite.ui.screens.edit.components.TabContentWrapperCustomImage
import com.docscanlite.ui.screens.edit.components.frames.ImageTransform
import com.docscanlite.ui.screens.edit.components.frames.RectangularFrameOverlay
import com.docscanlite.ui.screens.edit.components.frames.ZoomablePreviewImage

/**
 * Crop Tab Content
 * Shows image with crop overlay and crop preset panel
 */
@Composable
fun CropTabContent(
    previewBitmap: Bitmap?,
    cropState: CropState,
    onPresetClick: (CropPreset, Int, Int) -> Unit,
    onBoundsChange: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract dimensions directly from the bitmap
    val imageWidth = previewBitmap?.width ?: 0
    val imageHeight = previewBitmap?.height ?: 0

    TabContentWrapperCustomImage(
        imageContent = {
            // Conditional rendering - CropEditor with overlay OR plain cached image
            if (cropState.mode != CropMode.NONE && cropState.bounds != null) {
                CropEditor(
                    previewBitmap = previewBitmap,
                    cropBounds = cropState.bounds,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    cropMode = cropState.mode,
                    aspectRatio = cropState.getCurrentAspectRatio(),
                    onBoundsChange = onBoundsChange
                )
            } else {
                // No crop mode - just show cached bitmap
                ZoomablePreviewImage(
                    previewBitmap = previewBitmap,
                    enableZoom = false
                )
            }
        },
        panelContent = {
            CropPanel(
                cropState = cropState,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                onPresetClick = onPresetClick
            )
        },
        modifier = modifier
    )
}

/**
 * Crop editor with cached bitmap and rectangular frame overlay
 */
@Composable
private fun CropEditor(
    previewBitmap: Bitmap?,
    cropBounds: RectF,
    imageWidth: Int,
    imageHeight: Int,
    cropMode: CropMode,
    aspectRatio: Float?,
    onBoundsChange: (RectF) -> Unit
) {
    // Transform state for future zoom/pan support
    var transform by remember { mutableStateOf(ImageTransform.Default) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Cached preview image
        ZoomablePreviewImage(
            previewBitmap = previewBitmap,
            transform = transform,
            enableZoom = false, // Disabled for now, can be enabled in the future
            onTransformChange = { transform = it }
        )

        // Layer 2: Rectangular frame overlay with draggable handles
        RectangularFrameOverlay(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            bounds = cropBounds,
            cropMode = cropMode,
            aspectRatio = aspectRatio,
            transform = transform,
            onBoundsChange = onBoundsChange
        )
    }
}

@Composable
private fun CropPanel(
    cropState: CropState,
    imageWidth: Int,
    imageHeight: Int,
    onPresetClick: (CropPreset, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Crop presets in order: NONE, A4, FREE, A5, 16:9, 3:4
    val presets = listOf(
        CropPreset.NONE,
        CropPreset.FREE,
        CropPreset.A4,
        CropPreset.A5,
        CropPreset.RATIO_16_9,
        CropPreset.RATIO_3_4
    )

    // Match panel height with FilterPanel - horizontal scrollable
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(presets.size) { index ->
            val preset = presets[index]
            FilterChip(
                selected = cropState.preset == preset,
                onClick = { onPresetClick(preset, imageWidth, imageHeight) },
                label = {
                    Text(
                        text = when {
                            preset == CropPreset.FREE && cropState.mode == CropMode.FREE ->
                                cropState.getDisplayText()
                            else ->
                                preset.getDisplayNameWithOrientation(cropState.isVertical)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            )
        }
    }
}
