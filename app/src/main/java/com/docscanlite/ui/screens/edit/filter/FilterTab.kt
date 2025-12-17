package com.docscanlite.ui.screens.edit.filter

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docscanlite.ui.screens.edit.FilterOption
import com.docscanlite.ui.screens.edit.components.TabContentWrapper

/**
 * Filter Tab Content
 * Shows image with filter preview and filter selection panel
 */
@Composable
fun FilterTabContent(
    imagePath: String?,
    previewBitmap: Bitmap?,
    selectedFilter: FilterOption,
    onFilterSelected: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    TabContentWrapper(
        imagePath = imagePath,
        previewBitmap = previewBitmap,
        panelContent = {
            FilterPanel(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected
            )
        },
        modifier = modifier
    )
}

@Composable
private fun FilterPanel(
    selectedFilter: FilterOption,
    onFilterSelected: (FilterOption) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(FilterOption.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}
