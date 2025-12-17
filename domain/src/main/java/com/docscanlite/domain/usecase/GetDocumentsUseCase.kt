package com.docscanlite.domain.usecase

import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all documents
 * Following Single Responsibility Principle
 */
class GetDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(): Flow<List<Document>> {
        return repository.getAllDocuments()
    }
}
