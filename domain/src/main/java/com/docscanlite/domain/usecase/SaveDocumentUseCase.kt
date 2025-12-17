package com.docscanlite.domain.usecase

import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import javax.inject.Inject

/**
 * Use case for saving a new document to the repository
 */
class SaveDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(document: Document): Result<Unit> {
        return documentRepository.insertDocument(document)
    }
}
