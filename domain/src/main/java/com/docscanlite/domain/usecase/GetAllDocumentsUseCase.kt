package com.docscanlite.domain.usecase

import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all documents from the repository
 */
class GetAllDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    operator fun invoke(): Flow<List<Document>> {
        return documentRepository.getAllDocuments()
    }
}
