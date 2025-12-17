package com.docscanlite.ui.screens.edit.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard frame wrapper for tab content with Image + Bottom Panel
 *
 * Provides consistent layout structure:
 * - Top: Image area (weight = 1f, fills remaining space)
 * - Bottom: Control panel (fixed height with elevation)
 *
 * Used across all edit tabs to maintain consistent UI structure
 *
 * @param imageContent Content for image area (preview, bounds editor, etc.)
 * @param panelContent Content for bottom control panel (filters, adjustments, etc.)
 * @param panelElevation Elevation for the panel (default: 8.dp)
 * @param modifier Modifier for the container
 */
@Composable
fun ImageFrame(
    imageContent: @Composable BoxScope.() -> Unit,
    panelContent: @Composable () -> Unit,
    panelElevation: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Image area - takes all remaining space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            imageContent()
        }

        // Bottom control panel with elevation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = panelElevation
        ) {
            panelContent()
        }
    }
}

/**
 * Simplified version without panel - just image content
 * Useful for tabs that don't need bottom controls
 */
@Composable
fun ImageFrameSimple(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}
