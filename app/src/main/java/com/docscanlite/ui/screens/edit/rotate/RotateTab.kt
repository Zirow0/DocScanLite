package com.docscanlite.ui.screens.edit.rotate

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docscanlite.ui.screens.edit.components.TabContentWrapper

/**
 * Rotate Tab Content
 * Shows image and rotation controls
 */
@Composable
fun RotateTabContent(
    imagePath: String?,
    currentAngle: Float,
    onRotate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    TabContentWrapper(
        imagePath = imagePath,
        previewBitmap = null,
        panelContent = {
            RotatePanel(
                currentAngle = currentAngle,
                onRotate = onRotate
            )
        },
        modifier = modifier
    )
}

@Composable
private fun RotatePanel(
    currentAngle: Float,
    onRotate: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotate left 90°
        IconButton(
            onClick = { onRotate(-90f) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                contentDescription = "Повернути вліво",
                modifier = Modifier.size(32.dp)
            )
        }

        // Current angle display
        Text(
            text = "${currentAngle.toInt()}°",
            style = MaterialTheme.typography.headlineMedium,
            color = if (currentAngle != 0f) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Rotate right 90°
        IconButton(
            onClick = { onRotate(90f) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = "Повернути вправо",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
