package com.docscanlite.ui.screens.edit.bounds

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.docscanlite.ui.screens.edit.components.ImageFrameSimple
import com.docscanlite.ui.screens.edit.components.frames.ImageTransform
import com.docscanlite.ui.screens.edit.components.frames.QuadrilateralFrameOverlay
import com.docscanlite.ui.screens.edit.components.frames.ZoomablePreviewImage

/**
 * Bounds Tab Content
 * Shows original image with draggable corner points for document detection
 */
@Composable
fun BoundsTabContent(
    previewBitmap: Bitmap?,
    imageWidth: Int,
    imageHeight: Int,
    corners: List<PointF>,
    onCornerDrag: (Int, PointF) -> Unit,
    modifier: Modifier = Modifier
) {
    ImageFrameSimple(modifier = modifier) {
        if (corners.size == 4) {
            BoundsEditor(
                previewBitmap = previewBitmap,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                corners = corners,
                onCornerDrag = onCornerDrag
            )
        } else {
            // Show cached bitmap without corners (while detecting)
            ZoomablePreviewImage(
                previewBitmap = previewBitmap,
                enableZoom = false
            )
        }
    }
}

@Composable
private fun BoundsEditor(
    previewBitmap: Bitmap?,
    imageWidth: Int,
    imageHeight: Int,
    corners: List<PointF>,
    onCornerDrag: (Int, PointF) -> Unit
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

        // Layer 2: Quadrilateral frame overlay with draggable corners
        QuadrilateralFrameOverlay(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            corners = corners,
            transform = transform,
            onCornerDrag = onCornerDrag
        )
    }
}
