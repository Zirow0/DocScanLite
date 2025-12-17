package com.docscanlite.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.docscanlite.ui.screens.boundsedit.BoundsEditScreen
import com.docscanlite.ui.screens.camera.CameraScreen
import com.docscanlite.ui.screens.document.DocumentDetailsScreen
import com.docscanlite.ui.screens.edit.EditScreen
import com.docscanlite.ui.screens.gallery.GalleryScreen
import com.docscanlite.ui.screens.settings.SettingsScreen
import com.docscanlite.ui.screens.splash.SplashScreen

/**
 * Main navigation graph for the application
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Destinations.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(route = Destinations.Splash.route) {
            SplashScreen(
                onNavigateToGallery = {
                    navController.navigate(Destinations.Gallery.route) {
                        popUpTo(Destinations.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Gallery Screen (Main screen)
        composable(route = Destinations.Gallery.route) {
            GalleryScreen(
                onNavigateToCamera = {
                    navController.navigate(Destinations.Camera.route)
                },
                onNavigateToDocument = { documentId ->
                    navController.navigate(Destinations.DocumentDetails.createRoute(documentId))
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.Settings.route)
                }
            )
        }

        // Camera Screen
        composable(route = Destinations.Camera.route) {
            CameraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPhotoTaken = { imagePath ->
                    navController.navigate(Destinations.EditDocument.createRouteFromCamera(imagePath))
                }
            )
        }

        // Bounds Edit Screen
        composable(
            route = Destinations.BoundsEdit.route,
            arguments = listOf(
                navArgument("imagePath") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            BoundsEditScreen(
                imagePath = imagePath,
                onConfirm = { documentId ->
                    navController.navigate(Destinations.DocumentDetails.createRoute(documentId)) {
                        popUpTo(Destinations.Gallery.route)
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Document Details Screen
        composable(
            route = Destinations.DocumentDetails.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            DocumentDetailsScreen(
                documentId = documentId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { docId ->
                    navController.navigate(Destinations.EditDocument.createRouteFromDocument(docId))
                }
            )
        }

        // Edit Document Screen
        composable(
            route = Destinations.EditDocument.route,
            arguments = listOf(
                navArgument("documentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("imagePath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            val imagePath = backStackEntry.arguments?.getString("imagePath")
            EditScreen(
                documentId = documentId,
                imagePath = imagePath,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSave = { savedDocumentId ->
                    navController.navigate(Destinations.DocumentDetails.createRoute(savedDocumentId)) {
                        popUpTo(Destinations.Gallery.route)
                    }
                }
            )
        }

        // Settings Screen
        composable(route = Destinations.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
