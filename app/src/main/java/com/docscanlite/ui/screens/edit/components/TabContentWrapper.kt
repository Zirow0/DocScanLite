package com.docscanlite.ui.screens.edit.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * High-level wrapper that combines ImageFrame + PreviewImage
 * for standard tabs (Filter, Adjust, Rotate)
 *
 * This is the most abstracted component that handles:
 * - Layout structure (Column with image + panel)
 * - Image preview logic (bitmap vs file)
 * - Panel placement with elevation
 *
 * Use this when you have a simple tab with:
 * - Image at top (with optional preview bitmap)
 * - Control panel at bottom
 *
 * @param imagePath Path to image file
 * @param previewBitmap Optional preview bitmap with filters/adjustments
 * @param panelContent Bottom control panel content
 * @param imageOverlay Optional overlay on top of image (for special cases)
 * @param modifier Modifier for the container
 */
@Composable
fun TabContentWrapper(
    imagePath: String?,
    previewBitmap: Bitmap?,
    panelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    imageOverlay: @Composable BoxScope.() -> Unit = {}
) {
    ImageFrame(
        modifier = modifier,
        imageContent = {
            PreviewImage(
                imagePath = imagePath,
                previewBitmap = previewBitmap
            )

            // Optional overlay (e.g., adjustment overlay indicator)
            imageOverlay()
        },
        panelContent = panelContent
    )
}

/**
 * Wrapper for tabs with conditional image rendering
 * (e.g., Crop tab - shows CropOverlay OR plain image)
 *
 * More flexible version that allows custom image content
 *
 * @param imageContent Custom image content (can be conditional)
 * @param panelContent Bottom control panel content
 * @param modifier Modifier for the container
 */
@Composable
fun TabContentWrapperCustomImage(
    imageContent: @Composable BoxScope.() -> Unit,
    panelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    ImageFrame(
        modifier = modifier,
        imageContent = imageContent,
        panelContent = panelContent
    )
}
