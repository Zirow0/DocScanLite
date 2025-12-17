package com.docscanlite.domain.repository

import com.docscanlite.domain.common.Result
import com.docscanlite.domain.model.Document
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Document operations
 * Domain layer contract to be implemented by data layer
 */
interface DocumentRepository {

    /**
     * Observe all documents in the database
     * @return Flow of list of documents, ordered by modified date descending
     */
    fun getAllDocuments(): Flow<List<Document>>

    /**
     * Get a single document by ID
     * @param documentId The document ID
     * @return Result containing the document or error
     */
    suspend fun getDocumentById(documentId: String): Result<Document>

    /**
     * Observe a single document by ID
     * @param documentId The document ID
     * @return Flow of document or null if not found
     */
    fun observeDocumentById(documentId: String): Flow<Document?>

    /**
     * Search documents by name or OCR text
     * @param query The search query
     * @return Flow of matching documents
     */
    fun searchDocuments(query: String): Flow<List<Document>>

    /**
     * Get documents by tag
     * @param tag The tag to filter by
     * @return Flow of documents with the specified tag
     */
    fun getDocumentsByTag(tag: String): Flow<List<Document>>

    /**
     * Insert a new document
     * @param document The document to insert
     * @return Result indicating success or error
     */
    suspend fun insertDocument(document: Document): Result<Unit>

    /**
     * Update an existing document
     * @param document The document to update
     * @return Result indicating success or error
     */
    suspend fun updateDocument(document: Document): Result<Unit>

    /**
     * Delete a document
     * @param documentId The ID of the document to delete
     * @return Result indicating success or error
     */
    suspend fun deleteDocument(documentId: String): Result<Unit>

    /**
     * Delete all documents
     * @return Result indicating success or error
     */
    suspend fun deleteAllDocuments(): Result<Unit>

    /**
     * Get the total count of documents
     * @return Result containing the count
     */
    suspend fun getDocumentCount(): Result<Int>

    /**
     * Observe the total count of documents
     * @return Flow of document count
     */
    fun observeDocumentCount(): Flow<Int>
}
