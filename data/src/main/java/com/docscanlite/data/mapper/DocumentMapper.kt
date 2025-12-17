package com.docscanlite.data.mapper

import com.docscanlite.data.local.entity.DocumentEntity
import com.docscanlite.domain.model.Document

/**
 * Mapper functions to convert between domain and data layer models
 */

fun DocumentEntity.toDomain(): Document {
    return Document(
        id = id,
        name = name,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        originalPath = originalPath,
        processedPath = processedPath,
        thumbnailPath = thumbnailPath,
        ocrText = ocrText,
        fileSize = fileSize,
        width = width,
        height = height,
        tags = tags,
        bounds = bounds,
        filterName = filterName,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        cropBounds = cropBounds,
        rotationAngle = rotationAngle
    )
}

fun Document.toEntity(): DocumentEntity {
    return DocumentEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        originalPath = originalPath,
        processedPath = processedPath,
        thumbnailPath = thumbnailPath,
        ocrText = ocrText,
        fileSize = fileSize,
        width = width,
        height = height,
        tags = tags,
        bounds = bounds,
        filterName = filterName,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        cropBounds = cropBounds,
        rotationAngle = rotationAngle
    )
}

fun List<DocumentEntity>.toDomain(): List<Document> {
    return map { it.toDomain() }
}

fun List<Document>.toEntity(): List<DocumentEntity> {
    return map { it.toEntity() }
}
