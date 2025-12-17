package com.docscanlite.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.docscanlite.ui.theme.DocScanLiteTheme

/**
 * Settings Screen - App settings and preferences
 * TODO: Implement settings functionality with DataStore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Налаштування",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Camera Settings Section
            Text(
                text = "Камера",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider()
            SettingsItem(
                title = "Автоматичне виявлення меж",
                subtitle = "Coming soon"
            )
            SettingsItem(
                title = "Автоматичний OCR",
                subtitle = "Coming soon"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image Settings Section
            Text(
                text = "Зображення",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider()
            SettingsItem(
                title = "Зберігати оригінал",
                subtitle = "Coming soon"
            )
            SettingsItem(
                title = "Якість зображення",
                subtitle = "Coming soon"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Export Settings Section
            Text(
                text = "Експорт",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider()
            SettingsItem(
                title = "Формат за замовчуванням",
                subtitle = "Coming soon"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Settings Section
            Text(
                text = "Вигляд",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider()
            SettingsItem(
                title = "Тема",
                subtitle = "Coming soon"
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    DocScanLiteTheme {
        SettingsScreen(onNavigateBack = {})
    }
}
