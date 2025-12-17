package com.docscanlite.ui.screens.edit.components.frames

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.docscanlite.ui.screens.edit.CropHandle
import com.docscanlite.ui.screens.edit.CropMode

/**
 * Rectangular frame overlay with 8 draggable handles (4 corners + 4 edges)
 * Used for crop functionality with aspect ratio constraints
 *
 * @param imageWidth Original image width in pixels
 * @param imageHeight Original image height in pixels
 * @param bounds Crop bounds in normalized coordinates (0..1)
 * @param cropMode Current crop mode (FREE or PRESET)
 * @param aspectRatio Aspect ratio constraint for PRESET mode (null for FREE mode)
 * @param transform Current image transformation state (for zoom/pan sync)
 * @param onBoundsChange Callback when bounds change with new normalized coordinates
 * @param frameColor Color of the frame
 * @param showOverlay Whether to show darkened overlay outside crop area
 * @param handleSize Size of the draggable handles
 */
@Composable
fun RectangularFrameOverlay(
    imageWidth: Int,
    imageHeight: Int,
    bounds: RectF,
    cropMode: CropMode,
    aspectRatio: Float?,
    transform: ImageTransform = ImageTransform.Default,
    onBoundsChange: (RectF) -> Unit,
    modifier: Modifier = Modifier,
    frameColor: Color = MaterialTheme.colorScheme.primary,
    showOverlay: Boolean = true,
    handleSize: Dp = 24.dp
) {
    val density = LocalDensity.current
    val handleSizePx = with(density) { handleSize.toPx() }
    val handleTouchRadius = with(density) { 48.dp.toPx() } // Material Design minimum

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Container dimensions in pixels
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // Calculate image display size after ContentScale.Fit
        val imageAspect = imageWidth.toFloat() / imageHeight.toFloat()
        val containerAspect = containerWidthPx / containerHeightPx

        val (displayWidthPx, displayHeightPx) = if (imageAspect > containerAspect) {
            containerWidthPx to containerWidthPx / imageAspect
        } else {
            containerHeightPx * imageAspect to containerHeightPx
        }

        val displayWidthDp = with(density) { displayWidthPx.toDp() }
        val displayHeightDp = with(density) { displayHeightPx.toDp() }

        // Scale factors (image pixels -> screen pixels)
        val scaleX = displayWidthPx / imageWidth
        val scaleY = displayHeightPx / imageHeight

        // Helper function to convert normalized bounds to screen coordinates
        fun calculateCropRect(normalizedBounds: RectF): RectF {
            // Convert normalized (0..1) to image pixels
            val imagePx = RectF(
                normalizedBounds.left * imageWidth,
                normalizedBounds.top * imageHeight,
                normalizedBounds.right * imageWidth,
                normalizedBounds.bottom * imageHeight
            )
            // Then scale to screen coordinates
            return RectF(
                imagePx.left * scaleX,
                imagePx.top * scaleY,
                imagePx.right * scaleX,
                imagePx.bottom * scaleY
            )
        }

        // Drag state (store in image pixel coordinates)
        var dragHandle by remember { mutableStateOf<CropHandle?>(null) }
        var dragStartBoundsImagePx by remember { mutableStateOf<RectF?>(null) }
        var currentCropBounds by remember { mutableStateOf(bounds) }

        // Update currentCropBounds when bounds or aspectRatio changes from outside
        LaunchedEffect(bounds, aspectRatio) {
            android.util.Log.d("CropOverlay", "LaunchedEffect triggered: dragHandle=$dragHandle, bounds=$bounds")
            if (dragHandle == null) {
                android.util.Log.d("CropOverlay", "Updating currentCropBounds from external bounds")
                currentCropBounds = bounds
            } else {
                android.util.Log.d("CropOverlay", "Ignoring external bounds update - drag in progress")
            }
        }

        // Overlay Canvas - same size as image, with transform applied
        Canvas(
            modifier = Modifier
                .width(displayWidthDp)
                .height(displayHeightDp)
                .graphicsLayer {
                    this.scaleX = transform.scale
                    this.scaleY = transform.scale
                    this.translationX = transform.offsetX
                    this.translationY = transform.offsetY
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val startTime = System.currentTimeMillis()
                            android.util.Log.d("CropOverlay", "=== onDragStart [time=$startTime] ===")
                            android.util.Log.d("CropOverlay", "offset: $offset")
                            android.util.Log.d("CropOverlay", "aspectRatio parameter at drag start: $aspectRatio")
                            android.util.Log.d("CropOverlay", "cropMode: $cropMode")
                            android.util.Log.d("CropOverlay", "currentCropBounds: $currentCropBounds")

                            // Adjust touch position for transform
                            val adjustedOffset = Offset(
                                (offset.x - transform.offsetX) / transform.scale,
                                (offset.y - transform.offsetY) / transform.scale
                            )

                            val currentCropRect = calculateCropRect(currentCropBounds)
                            dragHandle = detectHandle(adjustedOffset, currentCropRect, handleTouchRadius / transform.scale)

                            android.util.Log.d("CropOverlay", "detected handle: $dragHandle")

                            dragStartBoundsImagePx = RectF(
                                currentCropBounds.left * imageWidth,
                                currentCropBounds.top * imageHeight,
                                currentCropBounds.right * imageWidth,
                                currentCropBounds.bottom * imageHeight
                            )

                            android.util.Log.d("CropOverlay", "onDragStart complete - waiting for onDrag events...")
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val handle = dragHandle ?: return@detectDragGestures
                            val startBounds = dragStartBoundsImagePx ?: return@detectDragGestures

                            // Logging: onDrag START
                            android.util.Log.d("CropDragFREE", "=== onDrag START (mode=$cropMode) ===")
                            android.util.Log.d("CropDragFREE", "change: ${change.position}, dragAmount: $dragAmount")

                            // Adjust dragAmount for transform scale
                            val adjustedDragX = dragAmount.x / transform.scale
                            val adjustedDragY = dragAmount.y / transform.scale

                            // Logging: adjusted drag
                            android.util.Log.d("CropDragFREE", "transform.scale=${transform.scale}")
                            android.util.Log.d("CropDragFREE", "adjustedDrag: x=$adjustedDragX, y=$adjustedDragY")

                            // Convert from screen pixels to image pixels
                            val deltaImageX = adjustedDragX / scaleX
                            val deltaImageY = adjustedDragY / scaleY

                            // Logging: deltaImage
                            android.util.Log.d("CropDragFREE", "scaleX=$scaleX, scaleY=$scaleY")
                            android.util.Log.d("CropDragFREE", "deltaImage: x=$deltaImageX, y=$deltaImageY")

                            val newBounds = RectF(startBounds)

                            when (cropMode) {
                                CropMode.FREE -> {
                                    // Logging: BEFORE applyFreeDrag
                                    android.util.Log.d("CropDragFREE", "startBounds (image px): $startBounds")
                                    android.util.Log.d("CropDragFREE", "newBounds BEFORE applyFreeDrag: $newBounds")

                                    applyFreeDrag(handle, newBounds, deltaImageX, deltaImageY)

                                    // Logging: AFTER applyFreeDrag
                                    android.util.Log.d("CropDragFREE", "newBounds AFTER applyFreeDrag: $newBounds")
                                    android.util.Log.d("CropDragFREE", "  delta applied: left=${newBounds.left - startBounds.left}, top=${newBounds.top - startBounds.top}")

                                    // Clamp to image bounds with margin (for FREE mode)
                                    val minMargin = 10f
                                    newBounds.left = newBounds.left.coerceIn(minMargin, imageWidth.toFloat() - minMargin)
                                    newBounds.top = newBounds.top.coerceIn(minMargin, imageHeight.toFloat() - minMargin)
                                    newBounds.right = newBounds.right.coerceIn(minMargin, imageWidth.toFloat() - minMargin)
                                    newBounds.bottom = newBounds.bottom.coerceIn(minMargin, imageHeight.toFloat() - minMargin)

                                    // Logging: AFTER clamping
                                    android.util.Log.d("CropDragFREE", "newBounds AFTER clamping: $newBounds")
                                }
                                CropMode.PRESET -> {
                                    if (aspectRatio != null) {
                                        android.util.Log.d("CropOverlay", "onDrag: using aspectRatio=$aspectRatio, cropMode=$cropMode")
                                        applyPresetDrag(handle, newBounds, deltaImageX, deltaImageY, aspectRatio, imageWidth, imageHeight)
                                    }
                                }
                                else -> {}
                            }

                            // Update dragStart for next iteration (like original implementation)
                            if (cropMode == CropMode.FREE) {
                                android.util.Log.d("CropDragFREE", "Updating dragStartBoundsImagePx from $dragStartBoundsImagePx to $newBounds")
                            }
                            dragStartBoundsImagePx = newBounds

                            // Convert to normalized coordinates
                            val normalized = RectF(
                                newBounds.left / imageWidth,
                                newBounds.top / imageHeight,
                                newBounds.right / imageWidth,
                                newBounds.bottom / imageHeight
                            )

                            if (cropMode == CropMode.FREE) {
                                android.util.Log.d("CropDragFREE", "normalized: $normalized")
                                android.util.Log.d("CropDragFREE", "=== onDrag END ===\n")
                            }

                            currentCropBounds = normalized
                            onBoundsChange(normalized)
                        },
                        onDragEnd = {
                            val endTime = System.currentTimeMillis()
                            android.util.Log.d("CropOverlay", "=== onDragEnd [time=$endTime] ===")
                            android.util.Log.d("CropOverlay", "Final dragHandle: $dragHandle")
                            android.util.Log.d("CropOverlay", "Final bounds: $dragStartBoundsImagePx")
                            dragHandle = null
                            dragStartBoundsImagePx = null
                        }
                    )
                }
        ) {
            val drawCropRect = calculateCropRect(currentCropBounds)

            // Draw darkened overlay outside crop area
            if (showOverlay) {
                val overlayColor = Color.Black.copy(alpha = 0.25f)

                // Top
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, drawCropRect.top)
                )
                // Bottom
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0f, drawCropRect.bottom),
                    size = Size(size.width, size.height - drawCropRect.bottom)
                )
                // Left
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0f, drawCropRect.top),
                    size = Size(drawCropRect.left, drawCropRect.height())
                )
                // Right
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(drawCropRect.right, drawCropRect.top),
                    size = Size(size.width - drawCropRect.right, drawCropRect.height())
                )
            }

            // Draw crop frame
            drawRect(
                color = frameColor,
                topLeft = Offset(drawCropRect.left, drawCropRect.top),
                size = Size(drawCropRect.width(), drawCropRect.height()),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw corner handles
            val corners = listOf(
                Offset(drawCropRect.left, drawCropRect.top),
                Offset(drawCropRect.right, drawCropRect.top),
                Offset(drawCropRect.left, drawCropRect.bottom),
                Offset(drawCropRect.right, drawCropRect.bottom)
            )

            corners.forEach { corner ->
                drawCircle(
                    color = frameColor,
                    radius = handleSizePx / 2,
                    center = corner
                )
                drawCircle(
                    color = Color.White,
                    radius = handleSizePx / 4,
                    center = corner
                )
            }

            // Draw edge handles
            val edges = listOf(
                Offset((drawCropRect.left + drawCropRect.right) / 2, drawCropRect.top),
                Offset((drawCropRect.left + drawCropRect.right) / 2, drawCropRect.bottom),
                Offset(drawCropRect.left, (drawCropRect.top + drawCropRect.bottom) / 2),
                Offset(drawCropRect.right, (drawCropRect.top + drawCropRect.bottom) / 2)
            )

            edges.forEach { edge ->
                drawCircle(
                    color = frameColor,
                    radius = handleSizePx / 2,
                    center = edge
                )
                drawCircle(
                    color = Color.White,
                    radius = handleSizePx / 4,
                    center = edge
                )
            }
        }
    }
}

/**
 * Apply drag for FREE mode
 */
private fun applyFreeDrag(handle: CropHandle, bounds: RectF, deltaX: Float, deltaY: Float) {
    android.util.Log.d("applyFreeDrag", "handle=$handle, deltaX=$deltaX, deltaY=$deltaY")
    android.util.Log.d("applyFreeDrag", "bounds BEFORE: $bounds")

    when (handle) {
        CropHandle.TOP_LEFT -> {
            bounds.left += deltaX
            bounds.top += deltaY
        }
        CropHandle.TOP_RIGHT -> {
            bounds.right += deltaX
            bounds.top += deltaY
        }
        CropHandle.BOTTOM_LEFT -> {
            bounds.left += deltaX
            bounds.bottom += deltaY
        }
        CropHandle.BOTTOM_RIGHT -> {
            bounds.right += deltaX
            bounds.bottom += deltaY
        }
        CropHandle.TOP -> bounds.top += deltaY
        CropHandle.BOTTOM -> bounds.bottom += deltaY
        CropHandle.LEFT -> bounds.left += deltaX
        CropHandle.RIGHT -> bounds.right += deltaX
    }

    android.util.Log.d("applyFreeDrag", "bounds AFTER: $bounds")
}

/**
 * Apply drag for PRESET mode with aspect ratio constraint
 * Uses original simple logic from old CropOverlay
 */
private fun applyPresetDrag(
    handle: CropHandle,
    bounds: RectF,
    deltaX: Float,
    deltaY: Float,
    aspectRatio: Float,
    imageWidth: Int,
    imageHeight: Int
) {
    // Log input parameters
    android.util.Log.d("CropDrag", "=== applyPresetDrag START ===")
    android.util.Log.d("CropDrag", "handle=$handle, deltaX=$deltaX, deltaY=$deltaY, aspectRatio=$aspectRatio")
    android.util.Log.d("CropDrag", "imageSize: ${imageWidth}x${imageHeight}")
    android.util.Log.d("CropDrag", "bounds BEFORE: $bounds")
    android.util.Log.d("CropDrag", "  width=${bounds.right - bounds.left}, height=${bounds.bottom - bounds.top}")
    android.util.Log.d("CropDrag", "  current ratio=${(bounds.right - bounds.left) / (bounds.bottom - bounds.top)}")

    when (handle) {
        CropHandle.TOP -> {
            // Fix BOTTOM, move TOP, adjust LEFT/RIGHT symmetrically
            android.util.Log.d("CropDrag", "TOP handle: adjusting height, centering width")
            bounds.top += deltaY
            val newHeight = bounds.bottom - bounds.top
            val newWidth = newHeight * aspectRatio
            val centerX = (bounds.left + bounds.right) / 2f
            bounds.left = centerX - newWidth / 2f
            bounds.right = centerX + newWidth / 2f
            android.util.Log.d("CropDrag", "  newHeight=$newHeight, newWidth=$newWidth")
        }
        CropHandle.BOTTOM -> {
            // Fix TOP, move BOTTOM, adjust LEFT/RIGHT symmetrically
            bounds.bottom += deltaY
            val newHeight = bounds.bottom - bounds.top
            val newWidth = newHeight * aspectRatio
            val centerX = (bounds.left + bounds.right) / 2f
            bounds.left = centerX - newWidth / 2f
            bounds.right = centerX + newWidth / 2f
        }
        CropHandle.LEFT -> {
            // Fix RIGHT, move LEFT, adjust TOP/BOTTOM symmetrically
            bounds.left += deltaX
            val newWidth = bounds.right - bounds.left
            val newHeight = newWidth / aspectRatio
            val centerY = (bounds.top + bounds.bottom) / 2f
            bounds.top = centerY - newHeight / 2f
            bounds.bottom = centerY + newHeight / 2f
        }
        CropHandle.RIGHT -> {
            // Fix LEFT, move RIGHT, adjust TOP/BOTTOM symmetrically
            bounds.right += deltaX
            val newWidth = bounds.right - bounds.left
            val newHeight = newWidth / aspectRatio
            val centerY = (bounds.top + bounds.bottom) / 2f
            bounds.top = centerY - newHeight / 2f
            bounds.bottom = centerY + newHeight / 2f
        }
        CropHandle.TOP_LEFT -> {
            // Fix BOTTOM_RIGHT corner, move TOP_LEFT, maintain aspect ratio
            // Change both coordinates simultaneously
            android.util.Log.d("CropDrag", "TOP_LEFT corner: applying both deltaX and deltaY")
            bounds.left += deltaX
            bounds.top += deltaY

            // Calculate new dimensions according to proportions
            val potentialHeight = bounds.bottom - bounds.top
            val potentialWidth = bounds.right - bounds.left
            val potentialRatio = potentialWidth / potentialHeight

            android.util.Log.d("CropDrag", "  potentialWidth=$potentialWidth, potentialHeight=$potentialHeight")
            android.util.Log.d("CropDrag", "  potentialRatio=$potentialRatio vs targetRatio=$aspectRatio")

            // Determine which dimension to use as leading
            if (potentialRatio > aspectRatio) {
                // Too wide - adjust by width
                android.util.Log.d("CropDrag", "  TOO WIDE: adjusting by width")
                val newWidth = bounds.right - bounds.left
                val newHeight = newWidth / aspectRatio
                bounds.top = bounds.bottom - newHeight
                android.util.Log.d("CropDrag", "  newWidth=$newWidth, newHeight=$newHeight")
            } else {
                // Too tall - adjust by height
                android.util.Log.d("CropDrag", "  TOO TALL: adjusting by height")
                val newHeight = bounds.bottom - bounds.top
                val newWidth = newHeight * aspectRatio
                bounds.left = bounds.right - newWidth
                android.util.Log.d("CropDrag", "  newHeight=$newHeight, newWidth=$newWidth")
            }
        }
        CropHandle.TOP_RIGHT -> {
            // Fix BOTTOM_LEFT corner, move TOP_RIGHT, maintain aspect ratio
            android.util.Log.d("CropDrag", "TOP_RIGHT corner")
            bounds.right += deltaX
            bounds.top += deltaY

            val potentialHeight = bounds.bottom - bounds.top
            val potentialWidth = bounds.right - bounds.left
            val potentialRatio = potentialWidth / potentialHeight
            android.util.Log.d("CropDrag", "  potentialRatio=$potentialRatio vs target=$aspectRatio")

            if (potentialRatio > aspectRatio) {
                android.util.Log.d("CropDrag", "  adjusting by width")
                val newWidth = bounds.right - bounds.left
                val newHeight = newWidth / aspectRatio
                bounds.top = bounds.bottom - newHeight
            } else {
                android.util.Log.d("CropDrag", "  adjusting by height")
                val newHeight = bounds.bottom - bounds.top
                val newWidth = newHeight * aspectRatio
                bounds.right = bounds.left + newWidth
            }
        }
        CropHandle.BOTTOM_LEFT -> {
            // Fix TOP_RIGHT corner, move BOTTOM_LEFT, maintain aspect ratio
            android.util.Log.d("CropDrag", "BOTTOM_LEFT corner")
            bounds.left += deltaX
            bounds.bottom += deltaY

            val potentialHeight = bounds.bottom - bounds.top
            val potentialWidth = bounds.right - bounds.left
            val potentialRatio = potentialWidth / potentialHeight
            android.util.Log.d("CropDrag", "  potentialRatio=$potentialRatio vs target=$aspectRatio")

            if (potentialRatio > aspectRatio) {
                android.util.Log.d("CropDrag", "  adjusting by width")
                val newWidth = bounds.right - bounds.left
                val newHeight = newWidth / aspectRatio
                bounds.bottom = bounds.top + newHeight
            } else {
                android.util.Log.d("CropDrag", "  adjusting by height")
                val newHeight = bounds.bottom - bounds.top
                val newWidth = newHeight * aspectRatio
                bounds.left = bounds.right - newWidth
            }
        }
        CropHandle.BOTTOM_RIGHT -> {
            // Fix TOP_LEFT corner, move BOTTOM_RIGHT, maintain aspect ratio
            android.util.Log.d("CropDrag", "BOTTOM_RIGHT corner")
            bounds.right += deltaX
            bounds.bottom += deltaY

            val potentialHeight = bounds.bottom - bounds.top
            val potentialWidth = bounds.right - bounds.left
            val potentialRatio = potentialWidth / potentialHeight
            android.util.Log.d("CropDrag", "  potentialRatio=$potentialRatio vs target=$aspectRatio")

            if (potentialRatio > aspectRatio) {
                android.util.Log.d("CropDrag", "  adjusting by width")
                val newWidth = bounds.right - bounds.left
                val newHeight = newWidth / aspectRatio
                bounds.bottom = bounds.top + newHeight
            } else {
                android.util.Log.d("CropDrag", "  adjusting by height")
                val newHeight = bounds.bottom - bounds.top
                val newWidth = newHeight * aspectRatio
                bounds.right = bounds.left + newWidth
            }
        }
    }

    // Additional check: ensure minimum dimensions
    val minDimension = 10f
    val currentWidth = bounds.right - bounds.left
    val currentHeight = bounds.bottom - bounds.top

    android.util.Log.d("CropDrag", "After handle logic:")
    android.util.Log.d("CropDrag", "  bounds=$bounds")
    android.util.Log.d("CropDrag", "  width=$currentWidth, height=$currentHeight, ratio=${currentWidth/currentHeight}")

    // If aspectRatio > 1 (width greater than height), height can be less than width
    // This is normal. But we must guarantee minimum dimensions
    if (currentWidth < minDimension || currentHeight < minDimension) {
        android.util.Log.d("CropDrag", "MINIMUM DIMENSION CHECK: width=$currentWidth, height=$currentHeight")
        // If one dimension is too small, set minimum dimensions
        if (currentWidth < minDimension) {
            android.util.Log.d("CropDrag", "  Width too small, setting to minimum")
            val centerX = (bounds.left + bounds.right) / 2f
            bounds.left = centerX - minDimension / 2f
            bounds.right = centerX + minDimension / 2f
            bounds.bottom = bounds.top + minDimension / aspectRatio
        }
        if (currentHeight < minDimension) {
            android.util.Log.d("CropDrag", "  Height too small, setting to minimum")
            val centerY = (bounds.top + bounds.bottom) / 2f
            bounds.top = centerY - minDimension / 2f
            bounds.bottom = centerY + minDimension / 2f
            bounds.right = bounds.left + minDimension * aspectRatio
        }
    }

    // Standard bounds clamping
    android.util.Log.d("CropDrag", "Before clamping: $bounds")
    bounds.left = bounds.left.coerceIn(minDimension, imageWidth - minDimension)
    bounds.top = bounds.top.coerceIn(minDimension, imageHeight - minDimension)
    bounds.right = bounds.right.coerceIn(minDimension, imageWidth - minDimension)
    bounds.bottom = bounds.bottom.coerceIn(minDimension, imageHeight - minDimension)

    val finalWidth = bounds.right - bounds.left
    val finalHeight = bounds.bottom - bounds.top
    val finalRatio = finalWidth / finalHeight

    android.util.Log.d("CropDrag", "=== applyPresetDrag END ===")
    android.util.Log.d("CropDrag", "bounds AFTER: $bounds")
    android.util.Log.d("CropDrag", "  finalWidth=$finalWidth, finalHeight=$finalHeight")
    android.util.Log.d("CropDrag", "  finalRatio=$finalRatio vs targetRatio=$aspectRatio")
    android.util.Log.d("CropDrag", "  ratio preserved: ${kotlin.math.abs(finalRatio - aspectRatio) < 0.01}")
    android.util.Log.d("CropDrag", "")
}

/**
 * Detect which handle was touched
 */
private fun detectHandle(
    touchPos: Offset,
    cropRect: RectF,
    touchRadius: Float
): CropHandle? {
    val centerX = (cropRect.left + cropRect.right) / 2
    val centerY = (cropRect.top + cropRect.bottom) / 2

    // Check corners first
    if ((touchPos - Offset(cropRect.left, cropRect.top)).getDistance() < touchRadius) {
        return CropHandle.TOP_LEFT
    }
    if ((touchPos - Offset(cropRect.right, cropRect.top)).getDistance() < touchRadius) {
        return CropHandle.TOP_RIGHT
    }
    if ((touchPos - Offset(cropRect.left, cropRect.bottom)).getDistance() < touchRadius) {
        return CropHandle.BOTTOM_LEFT
    }
    if ((touchPos - Offset(cropRect.right, cropRect.bottom)).getDistance() < touchRadius) {
        return CropHandle.BOTTOM_RIGHT
    }

    // Check edges
    if ((touchPos - Offset(centerX, cropRect.top)).getDistance() < touchRadius) {
        return CropHandle.TOP
    }
    if ((touchPos - Offset(centerX, cropRect.bottom)).getDistance() < touchRadius) {
        return CropHandle.BOTTOM
    }
    if ((touchPos - Offset(cropRect.left, centerY)).getDistance() < touchRadius) {
        return CropHandle.LEFT
    }
    if ((touchPos - Offset(cropRect.right, centerY)).getDistance() < touchRadius) {
        return CropHandle.RIGHT
    }

    return null
}
