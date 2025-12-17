package com.docscanlite.data.repository

import com.docscanlite.data.local.dao.DocumentDao
import com.docscanlite.data.mapper.toDomain
import com.docscanlite.data.mapper.toEntity
import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.Document
import com.docscanlite.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DocumentRepository
 * Handles data operations for documents using Room database
 */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao
) : DocumentRepository {

    override fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun getDocumentById(documentId: String): Result<Document> {
        return try {
            val entity = documentDao.getDocumentById(documentId)
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Error(NoSuchElementException("Document with ID $documentId not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeDocumentById(documentId: String): Flow<Document?> {
        return documentDao.observeDocumentById(documentId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun searchDocuments(query: String): Flow<List<Document>> {
        return documentDao.searchDocuments(query).map { entities ->
            entities.toDomain()
        }
    }

    override fun getDocumentsByTag(tag: String): Flow<List<Document>> {
        return documentDao.getDocumentsByTag(tag).map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun insertDocument(document: Document): Result<Unit> {
        return try {
            documentDao.insertDocument(document.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateDocument(document: Document): Result<Unit> {
        return try {
            documentDao.updateDocument(document.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            documentDao.deleteDocumentById(documentId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAllDocuments(): Result<Unit> {
        return try {
            documentDao.deleteAllDocuments()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getDocumentCount(): Result<Int> {
        return try {
            val count = documentDao.getDocumentCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeDocumentCount(): Flow<Int> {
        return documentDao.observeDocumentCount()
    }
}
