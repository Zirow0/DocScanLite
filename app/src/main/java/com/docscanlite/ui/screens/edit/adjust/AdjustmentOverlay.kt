package com.docscanlite.ui.screens.edit.adjust

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Adjustment parameter enum
 */
enum class AdjustParameter(val displayName: String) {
    BRIGHTNESS("Яскравість"),
    CONTRAST("Контраст"),
    SATURATION("Насиченість")
}

/**
 * State for adjustment overlay
 */
data class AdjustmentOverlayState(
    val parameter: AdjustParameter,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val formatValue: (Float) -> String
)

/**
 * Overlay shown during adjustment with animated arrows and auto-hide
 */
@Composable
fun AdjustmentOverlay(
    parameter: AdjustParameter,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-hide after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }

    // Animated progress value for smooth indicator
    val animatedProgress by animateFloatAsState(
        targetValue = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 50),
        label = "progress"
    )

    // Pulsing arrow animation (up-down movement)
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_pulse")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Top animated arrow (pulsing up)
            Text(
                text = "▲",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(y = arrowOffset.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (parameter) {
                        AdjustParameter.BRIGHTNESS -> Icons.Default.WbSunny
                        AdjustParameter.CONTRAST -> Icons.Default.Contrast
                        AdjustParameter.SATURATION -> Icons.Default.WaterDrop
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = parameter.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = formatValue(value),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Animated progress indicator
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom animated arrow (pulsing down)
            Text(
                text = "▼",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(y = (-arrowOffset).dp)
            )
        }
    }
}
