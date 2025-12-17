package com.docscanlite

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for DocScan Lite
 * Initializes Hilt and other app-level dependencies
 */
@HiltAndroidApp
class DocScanApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // TODO: Initialize OpenCV when dependency is available
        // if (OpenCVLoader.initDebug()) {
        //     Timber.d("OpenCV loaded successfully")
        // } else {
        //     Timber.e("OpenCV initialization failed")
        // }

        Timber.d("DocScan Lite app initialized")
    }
}
