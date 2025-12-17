package com.docscanlite.domain.usecase

import com.docscanlite.domain.common.Result
import com.docscanlite.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Use case for deleting a document from the repository
 */
class DeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(documentId: String): Result<Unit> {
        return documentRepository.deleteDocument(documentId)
    }
}
