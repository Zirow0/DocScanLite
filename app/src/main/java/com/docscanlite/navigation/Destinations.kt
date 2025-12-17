package com.docscanlite.navigation

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class Destinations(val route: String) {
    data object Splash : Destinations("splash")
    data object Gallery : Destinations("gallery")
    data object Camera : Destinations("camera")
    data object BoundsEdit : Destinations("bounds_edit?imagePath={imagePath}") {
        fun createRoute(imagePath: String) = "bounds_edit?imagePath=$imagePath"
    }
    data object DocumentDetails : Destinations("document/{documentId}") {
        fun createRoute(documentId: String) = "document/$documentId"
    }
    data object EditDocument : Destinations("edit?documentId={documentId}&imagePath={imagePath}") {
        fun createRoute(documentId: String? = null, imagePath: String? = null): String {
            val params = mutableListOf<String>()
            documentId?.let { params.add("documentId=$it") }
            imagePath?.let { params.add("imagePath=$it") }
            return "edit?${params.joinToString("&")}"
        }

        fun createRouteFromCamera(imagePath: String) = "edit?imagePath=$imagePath"
        fun createRouteFromDocument(documentId: String) = "edit?documentId=$documentId"
    }
    data object OCR : Destinations("ocr/{documentId}") {
        fun createRoute(documentId: String) = "ocr/$documentId"
    }
    data object Export : Destinations("export/{documentId}") {
        fun createRoute(documentId: String) = "export/$documentId"
    }
    data object Settings : Destinations("settings")
}
