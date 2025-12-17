package com.docscanlite.domain.model

/**
 * Supported export formats
 */
enum class ExportFormat {
    PDF,
    PDF_SEARCHABLE,
    JPEG,
    PNG,
    WEBP,
    TXT
}

/**
 * Export quality settings
 */
enum class ExportQuality {
    ORIGINAL,
    HIGH,
    MEDIUM,
    LOW
}
