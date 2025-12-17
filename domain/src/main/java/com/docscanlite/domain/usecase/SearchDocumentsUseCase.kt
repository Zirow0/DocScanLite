package com.docscanlite.domain.usecase

import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching documents by query
 */
class SearchDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    operator fun invoke(query: String): Flow<List<Document>> {
        return documentRepository.searchDocuments(query)
    }
}
