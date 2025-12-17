package com.docscanlite.ocr.di

import android.content.Context
import com.docscanlite.ocr.OcrProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for OCR dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideOcrProcessor(
        @ApplicationContext context: Context
    ): OcrProcessor {
        return OcrProcessor(context)
    }
}
