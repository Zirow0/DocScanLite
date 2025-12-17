package com.docscanlite.imageprocessing.detector

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Detects document boundaries in an image using edge detection and contour analysis
 */
@Singleton
class DocumentBoundsDetector @Inject constructor() {

    companion object {
        private const val TAG = "DocumentBoundsDetector"
    }

    data class DetectionResult(
        val corners: List<PointF>,
        val confidence: Float // 0.0 to 1.0
    )

    /**
     * Detect document bounds in bitmap
     * Returns four corners: top-left, top-right, bottom-right, bottom-left
     */
    fun detectBounds(bitmap: Bitmap): List<PointF> {
        val result = detectBoundsWithConfidence(bitmap)
        return result.corners
    }

    /**
     * Create debug visualization bitmap with filtered contours overlaid
     * @param bitmap Original image
     * @return Bitmap with large contours drawn in green
     */
    fun createDebugVisualization(bitmap: Bitmap): Bitmap {
        val debugBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(debugBitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(128, 0, 255, 0) // Яскравий зелений (50% прозорий)
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        try {
            // Downsample for faster processing
            val scaleFactor = calculateScaleFactor(bitmap)
            val scaledBitmap = if (scaleFactor < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scaleFactor).toInt(),
                    (bitmap.height * scaleFactor).toInt(),
                    true
                )
            } else {
                bitmap
            }

            // Detect edges and find contours
            val gray = convertToGrayscale(scaledBitmap)
            val edges = detectEdges(gray)
            val contours = findContours(edges)

            Log.d(TAG, "Found ${contours.size} contours for visualization")

            if (contours.isEmpty()) {
                Log.d(TAG, "No contours found, nothing to draw")
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                return debugBitmap
            }

            // Знаходимо максимальну кількість точок в контурі
            val maxContourSize = contours.maxOfOrNull { it.size } ?: 0
            Log.d(TAG, "Maximum contour size: $maxContourSize")

            // Поріг: 20% від максимального розміру
            val sizeThreshold = (maxContourSize * 0.20).toInt()
            Log.d(TAG, "Size threshold (20% of max): $sizeThreshold")

            // Фільтруємо контури
            val filteredContours = contours.filter { it.size >= sizeThreshold }
            Log.d(TAG, "Drawing ${filteredContours.size} filtered contours (out of ${contours.size})")

            // Малюємо відфільтровані контури
            for (contour in filteredContours) {
                val path = android.graphics.Path()

                // Масштабуємо точки назад до оригінального розміру
                val scaledContour = if (scaleFactor < 1.0f) {
                    contour.map { PointF(it.x / scaleFactor, it.y / scaleFactor) }
                } else {
                    contour
                }

                // Створюємо шлях
                if (scaledContour.isNotEmpty()) {
                    path.moveTo(scaledContour[0].x, scaledContour[0].y)

                    for (i in 1 until scaledContour.size) {
                        path.lineTo(scaledContour[i].x, scaledContour[i].y)
                    }

                    path.close()
                    canvas.drawPath(path, paint)
                }
            }

            if (scaledBitmap != bitmap) scaledBitmap.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Error creating debug visualization", e)
        }

        return debugBitmap
    }

    /**
     * Detect document bounds with confidence score
     * Returns corners and confidence level (0.0 = default fallback, 1.0 = high confidence)
     */
    fun detectBoundsWithConfidence(bitmap: Bitmap): DetectionResult {
        try {
            Log.d(TAG, "Starting document detection for ${bitmap.width}x${bitmap.height} image")

            // Downsample for faster processing
            val scaleFactor = calculateScaleFactor(bitmap)
            Log.d(TAG, "Scale factor: $scaleFactor")

            val scaledBitmap = if (scaleFactor < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scaleFactor).toInt(),
                    (bitmap.height * scaleFactor).toInt(),
                    true
                )
            } else {
                bitmap
            }

            // Step 1: Preprocess image
            val gray = convertToGrayscale(scaledBitmap)
            Log.d(TAG, "Converted to grayscale")

            // Step 2: Detect edges
            val edges = detectEdges(gray)
            val edgeCount = edges.sumOf { row -> row.count { it } }
            Log.d(TAG, "Detected $edgeCount edge pixels")

            // Step 3: Find contours
            val contours = findContours(edges)
            Log.d(TAG, "Found ${contours.size} contours")

            // Step 4: Find largest quadrilateral
            val quad = findLargestQuadrilateral(contours, scaledBitmap.width, scaledBitmap.height)

            if (quad != null) {
                Log.d(TAG, "Found quadrilateral with ${quad.size} points")
                val isValid = isValidQuadrilateral(quad, scaledBitmap.width, scaledBitmap.height)
                Log.d(TAG, "Quadrilateral valid: $isValid")

                if (isValid) {
                    // Scale corners back to original size
                    val scaledCorners = if (scaleFactor < 1.0f) {
                        quad.map { PointF(it.x / scaleFactor, it.y / scaleFactor) }
                    } else {
                        quad
                    }
                    val orderedCorners = orderCorners(scaledCorners)
                    val confidence = calculateConfidence(orderedCorners, bitmap.width, bitmap.height)
                    Log.d(TAG, "Detection successful with confidence: $confidence")

                    if (scaledBitmap != bitmap) scaledBitmap.recycle()
                    return DetectionResult(orderedCorners, confidence)
                }
            } else {
                Log.d(TAG, "No quadrilateral found")
            }

            if (scaledBitmap != bitmap) scaledBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error during document detection", e)
            e.printStackTrace()
        }

        // Fallback to default corners with low confidence
        Log.d(TAG, "Falling back to default corners")
        return DetectionResult(getDefaultCorners(bitmap.width, bitmap.height), 0.0f)
    }

    /**
     * Calculate appropriate scale factor for processing
     */
    private fun calculateScaleFactor(bitmap: Bitmap): Float {
        val maxDimension = 800
        val largestDimension = kotlin.math.max(bitmap.width, bitmap.height)
        return if (largestDimension > maxDimension) {
            maxDimension.toFloat() / largestDimension
        } else {
            1.0f
        }
    }

    /**
     * Convert image to grayscale
     */
    private fun convertToGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val gray = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Luminosity method
                gray[y][x] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }

        return gray
    }

    /**
     * Step 1: Apply Gaussian blur to reduce noise
     * Uses 5x5 Gaussian kernel
     */
    private fun applyGaussianBlur(gray: Array<IntArray>): Array<IntArray> {
        val height = gray.size
        val width = gray[0].size
        val blurred = Array(height) { IntArray(width) }

        // 5x5 Gaussian kernel (sigma ≈ 1.4)
        val kernel = arrayOf(
            intArrayOf(2, 4, 5, 4, 2),
            intArrayOf(4, 9, 12, 9, 4),
            intArrayOf(5, 12, 15, 12, 5),
            intArrayOf(4, 9, 12, 9, 4),
            intArrayOf(2, 4, 5, 4, 2)
        )
        val kernelSum = 159 // Sum of all kernel values

        for (y in 2 until height - 2) {
            for (x in 2 until width - 2) {
                var sum = 0
                for (ky in 0..4) {
                    for (kx in 0..4) {
                        sum += gray[y + ky - 2][x + kx - 2] * kernel[ky][kx]
                    }
                }
                blurred[y][x] = sum / kernelSum
            }
        }

        // Copy borders
        for (y in 0 until 2) {
            for (x in 0 until width) {
                blurred[y][x] = gray[y][x]
                blurred[height - 1 - y][x] = gray[height - 1 - y][x]
            }
        }
        for (x in 0 until 2) {
            for (y in 0 until height) {
                blurred[y][x] = gray[y][x]
                blurred[y][width - 1 - x] = gray[y][width - 1 - x]
            }
        }

        return blurred
    }

    /**
     * Step 2: Calculate gradient magnitude and direction using Sobel operator
     */
    private data class GradientData(
        val magnitude: Array<IntArray>,
        val direction: Array<IntArray>
    )

    private fun calculateGradients(gray: Array<IntArray>): GradientData {
        val height = gray.size
        val width = gray[0].size
        val magnitude = Array(height) { IntArray(width) }
        val direction = Array(height) { IntArray(width) }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                // Sobel operators for X and Y
                val gx = -gray[y-1][x-1] + gray[y-1][x+1] -
                        2*gray[y][x-1] + 2*gray[y][x+1] -
                        gray[y+1][x-1] + gray[y+1][x+1]

                val gy = -gray[y-1][x-1] - 2*gray[y-1][x] - gray[y-1][x+1] +
                        gray[y+1][x-1] + 2*gray[y+1][x] + gray[y+1][x+1]

                magnitude[y][x] = sqrt((gx * gx + gy * gy).toDouble()).toInt()

                // Calculate gradient direction (0-180 degrees, quantized to 4 directions)
                val angle = atan2(gy.toDouble(), gx.toDouble()) * 180 / Math.PI
                direction[y][x] = when {
                    angle < -157.5 || angle >= 157.5 -> 0  // Horizontal
                    angle >= -157.5 && angle < -112.5 -> 1 // Diagonal /
                    angle >= -112.5 && angle < -67.5 -> 2  // Vertical
                    angle >= -67.5 && angle < -22.5 -> 3   // Diagonal \
                    angle >= -22.5 && angle < 22.5 -> 0    // Horizontal
                    angle >= 22.5 && angle < 67.5 -> 3     // Diagonal \
                    angle >= 67.5 && angle < 112.5 -> 2    // Vertical
                    else -> 1                              // Diagonal /
                }
            }
        }

        return GradientData(magnitude, direction)
    }

    /**
     * Step 3: Non-maximum suppression
     * Thin edges by keeping only local maxima in gradient direction
     */
    private fun nonMaximumSuppression(gradientData: GradientData): Array<IntArray> {
        val magnitude = gradientData.magnitude
        val direction = gradientData.direction
        val height = magnitude.size
        val width = magnitude[0].size
        val suppressed = Array(height) { IntArray(width) }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val currentMag = magnitude[y][x]
                val dir = direction[y][x]

                // Get neighbor pixels in gradient direction
                val (neighbor1, neighbor2) = when (dir) {
                    0 -> Pair(magnitude[y][x-1], magnitude[y][x+1])     // Horizontal
                    1 -> Pair(magnitude[y-1][x-1], magnitude[y+1][x+1]) // Diagonal /
                    2 -> Pair(magnitude[y-1][x], magnitude[y+1][x])     // Vertical
                    else -> Pair(magnitude[y-1][x+1], magnitude[y+1][x-1]) // Diagonal \
                }

                // Keep only if local maximum
                suppressed[y][x] = if (currentMag >= neighbor1 && currentMag >= neighbor2) {
                    currentMag
                } else {
                    0
                }
            }
        }

        return suppressed
    }

    /**
     * Step 4: Hysteresis thresholding
     * Use two thresholds to classify pixels as strong, weak, or non-edge
     */
    private fun hysteresisThresholding(suppressed: Array<IntArray>): Array<BooleanArray> {
        val height = suppressed.size
        val width = suppressed[0].size
        val edges = Array(height) { BooleanArray(width) }

        // Calculate thresholds based on gradient distribution
        val maxGradient = suppressed.maxOfOrNull { row -> row.maxOrNull() ?: 0 } ?: 0
        val highThreshold = (maxGradient * 0.15).toInt() // 15% of max
        val lowThreshold = (highThreshold * 0.4).toInt()  // 40% of high threshold

        Log.d(TAG, "Hysteresis thresholds - Low: $lowThreshold, High: $highThreshold")

        // Mark strong edges
        val strongEdges = Array(height) { BooleanArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (suppressed[y][x] >= highThreshold) {
                    strongEdges[y][x] = true
                    edges[y][x] = true
                }
            }
        }

        // Track weak edges connected to strong edges
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                if (suppressed[y][x] >= lowThreshold && suppressed[y][x] < highThreshold) {
                    // Check if connected to strong edge (8-connectivity)
                    var connectedToStrong = false
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (strongEdges[y + dy][x + dx]) {
                                connectedToStrong = true
                                break
                            }
                        }
                        if (connectedToStrong) break
                    }

                    if (connectedToStrong) {
                        edges[y][x] = true
                    }
                }
            }
        }

        return edges
    }

    /**
     * Step 5: Morphological closing to fill gaps
     */
    private fun morphologicalClosing(edges: Array<BooleanArray>): Array<BooleanArray> {
        val height = edges.size
        val width = edges[0].size

        // Dilation followed by erosion
        val dilated = dilate(edges, width, height)
        return erode(dilated, width, height)
    }

    private fun dilate(edges: Array<BooleanArray>, width: Int, height: Int): Array<BooleanArray> {
        val result = Array(height) { BooleanArray(width) }
        val kernelSize = 2 // 5x5 kernel

        for (y in kernelSize until height - kernelSize) {
            for (x in kernelSize until width - kernelSize) {
                var hasEdge = false
                for (ky in -kernelSize..kernelSize) {
                    for (kx in -kernelSize..kernelSize) {
                        if (edges[y + ky][x + kx]) {
                            hasEdge = true
                            break
                        }
                    }
                    if (hasEdge) break
                }
                result[y][x] = hasEdge
            }
        }

        return result
    }

    private fun erode(edges: Array<BooleanArray>, width: Int, height: Int): Array<BooleanArray> {
        val result = Array(height) { BooleanArray(width) }
        val kernelSize = 2 // 5x5 kernel

        for (y in kernelSize until height - kernelSize) {
            for (x in kernelSize until width - kernelSize) {
                var allEdges = true
                for (ky in -kernelSize..kernelSize) {
                    for (kx in -kernelSize..kernelSize) {
                        if (!edges[y + ky][x + kx]) {
                            allEdges = false
                            break
                        }
                    }
                    if (!allEdges) break
                }
                result[y][x] = allEdges
            }
        }

        return result
    }

    /**
     * Edge detection using full Canny algorithm
     * Steps: Gaussian blur → Gradients → Non-max suppression → Hysteresis → Morphological closing
     */
    private fun detectEdges(gray: Array<IntArray>): Array<BooleanArray> {
        Log.d(TAG, "Step 1: Applying Gaussian blur")
        val blurred = applyGaussianBlur(gray)

        Log.d(TAG, "Step 2: Calculating gradients")
        val gradientData = calculateGradients(blurred)

        Log.d(TAG, "Step 3: Non-maximum suppression")
        val suppressed = nonMaximumSuppression(gradientData)

        Log.d(TAG, "Step 4: Hysteresis thresholding")
        val edges = hysteresisThresholding(suppressed)

        Log.d(TAG, "Step 5: Morphological closing")
        val closed = morphologicalClosing(edges)

        val edgeCount = closed.sumOf { row -> row.count { it } }
        Log.d(TAG, "Canny edge detection complete. Edge pixels: $edgeCount")

        return closed
    }

    /**
     * Find contours in edge-detected image
     */
    private fun findContours(edges: Array<BooleanArray>): List<List<PointF>> {
        val height = edges.size
        val width = edges[0].size
        val visited = Array(height) { BooleanArray(width) }
        val contours = mutableListOf<List<PointF>>()

        // Don't skip pixels - check every pixel
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (edges[y][x] && !visited[y][x]) {
                    val contour = traceContour(edges, visited, x, y, width, height)
                    if (contour.size >= 20) { // Need at least 20 points for a meaningful contour
                        contours.add(contour)
                        Log.d(TAG, "Found contour with ${contour.size} points")
                    }
                }
            }
        }

        Log.d(TAG, "Total contours found: ${contours.size}")
        return contours
    }

    /**
     * Trace contour starting from point using flood fill
     */
    private fun traceContour(
        edges: Array<BooleanArray>,
        visited: Array<BooleanArray>,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): List<PointF> {
        val contour = mutableListOf<PointF>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startX, startY))

        while (queue.isNotEmpty() && contour.size < 5000) {
            val (x, y) = queue.removeFirst()

            if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x] || !edges[y][x]) {
                continue
            }

            visited[y][x] = true
            contour.add(PointF(x.toFloat(), y.toFloat()))

            // Add 8-connected neighbors for better connectivity
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx != 0 || dy != 0) {
                        queue.add(Pair(x + dx, y + dy))
                    }
                }
            }
        }

        return contour
    }

    /**
     * Data class for quadrilateral candidates with quality metrics
     */
    private data class QuadCandidate(
        val quad: List<PointF>,
        val area: Double,
        val aspectRatio: Double,
        val convexity: Double,
        val score: Double
    )

    /**
     * Find best quadrilateral from contours using quality scoring
     */
    private fun findLargestQuadrilateral(
        contours: List<List<PointF>>,
        width: Int,
        height: Int
    ): List<PointF>? {
        val candidates = mutableListOf<QuadCandidate>()
        val imageArea = width * height

        Log.d(TAG, "Analyzing ${contours.size} contours for quadrilateral candidates")

        for (contour in contours) {
            if (contour.size < 4) continue

            // Simplify contour to polygon
            val polygon = approximatePolygon(contour, width, height)

            // Check if it's a quadrilateral
            if (polygon.size == 4) {
                val area = calculateArea(polygon)
                val areaRatio = area / imageArea

                // Filter out too small or too large quads early
                if (areaRatio < 0.08 || areaRatio > 0.99) {
                    continue
                }

                val aspectRatio = calculateAspectRatio(polygon)
                val convexity = calculateConvexity(polygon)

                // Calculate quality score
                val score = calculateQuadScore(
                    areaRatio = areaRatio,
                    aspectRatio = aspectRatio,
                    convexity = convexity
                )

                candidates.add(QuadCandidate(polygon, area, aspectRatio, convexity, score))

                Log.d(TAG, "Quad candidate - Area: ${(areaRatio * 100).toInt()}%, " +
                        "Aspect: ${"%.2f".format(aspectRatio)}, " +
                        "Convexity: ${"%.2f".format(convexity)}, " +
                        "Score: ${"%.2f".format(score)}")
            }
        }

        if (candidates.isEmpty()) {
            Log.d(TAG, "No valid quadrilateral candidates found")
            return null
        }

        // Sort by score (highest first)
        val best = candidates.maxByOrNull { it.score }

        if (best != null) {
            Log.d(TAG, "Selected best quad with score: ${"%.2f".format(best.score)}")
        }

        return best?.quad
    }

    /**
     * Calculate aspect ratio of quadrilateral (width/height)
     * Ideal for documents: 1.0 (square) to 1.414 (A4)
     */
    private fun calculateAspectRatio(quad: List<PointF>): Double {
        if (quad.size != 4) return 0.0

        // Order corners to get correct sides
        val ordered = orderCorners(quad)

        val topWidth = distance(ordered[0], ordered[1])
        val bottomWidth = distance(ordered[2], ordered[3])
        val leftHeight = distance(ordered[0], ordered[3])
        val rightHeight = distance(ordered[1], ordered[2])

        val width = (topWidth + bottomWidth) / 2.0
        val height = (leftHeight + rightHeight) / 2.0

        return if (height > 0) width / height else 0.0
    }

    /**
     * Calculate distance between two points
     */
    private fun distance(p1: PointF, p2: PointF): Double {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt((dx * dx + dy * dy).toDouble())
    }

    /**
     * Calculate convexity of quadrilateral (should be close to 1.0 for documents)
     * Compares actual area to convex hull area
     */
    private fun calculateConvexity(quad: List<PointF>): Double {
        if (quad.size != 4) return 0.0

        val area = calculateArea(quad)
        val hull = convexHull(quad)
        val hullArea = calculateArea(hull)

        return if (hullArea > 0) area / hullArea else 0.0
    }

    /**
     * Calculate quality score for quadrilateral candidate
     * Higher score = better document candidate
     */
    private fun calculateQuadScore(
        areaRatio: Double,
        aspectRatio: Double,
        convexity: Double
    ): Double {
        // Area score (prefer 25-85% of image)
        val areaScore = when {
            areaRatio in 0.25..0.85 -> 1.0
            areaRatio in 0.15..0.25 -> 0.7 + (areaRatio - 0.15) * 3.0
            areaRatio in 0.85..0.95 -> 1.0 - (areaRatio - 0.85) * 3.0
            areaRatio in 0.10..0.15 -> 0.4 + (areaRatio - 0.10) * 6.0
            else -> 0.2
        }

        // Aspect ratio score (prefer document-like ratios)
        // Common ratios: 1.0 (square), 1.294 (A4 portrait), 1.414 (√2)
        val aspectScore = when {
            aspectRatio in 0.7..1.5 -> 1.0  // Good for most documents
            aspectRatio in 0.5..0.7 || aspectRatio in 1.5..2.0 -> 0.7
            aspectRatio in 0.3..0.5 || aspectRatio in 2.0..3.0 -> 0.4
            else -> 0.1
        }

        // Convexity score (should be very close to 1.0)
        val convexityScore = when {
            convexity > 0.95 -> 1.0
            convexity > 0.9 -> 0.8
            convexity > 0.8 -> 0.5
            else -> 0.2
        }

        // Weighted combination
        return areaScore * 0.4 + aspectScore * 0.3 + convexityScore * 0.3
    }

    /**
     * Approximate contour as polygon using convex hull + simplification
     */
    private fun approximatePolygon(contour: List<PointF>, width: Int, height: Int): List<PointF> {
        if (contour.size < 4) return contour

        // Find convex hull
        val hull = convexHull(contour)

        if (hull.size == 4) return hull

        // Simplify to 4 corners
        return simplifyToQuadrilateral(hull)
    }

    /**
     * Graham scan algorithm for convex hull
     */
    private fun convexHull(points: List<PointF>): List<PointF> {
        if (points.size < 3) return points

        // Find bottom-most point
        val start = points.minByOrNull { it.y } ?: points[0]

        // Sort by polar angle
        val sorted = points.sortedBy { p ->
            if (p == start) -Double.MAX_VALUE
            else atan2((p.y - start.y).toDouble(), (p.x - start.x).toDouble())
        }

        val hull = mutableListOf<PointF>()

        for (point in sorted) {
            while (hull.size >= 2 && !isLeftTurn(hull[hull.size - 2], hull[hull.size - 1], point)) {
                hull.removeAt(hull.size - 1)
            }
            hull.add(point)
        }

        return hull
    }

    /**
     * Check if three points make a left turn
     */
    private fun isLeftTurn(p1: PointF, p2: PointF, p3: PointF): Boolean {
        return ((p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)) > 0
    }

    /**
     * Simplify polygon to quadrilateral by finding 4 most prominent corners
     */
    private fun simplifyToQuadrilateral(points: List<PointF>): List<PointF> {
        if (points.size <= 4) return points

        // Find 4 corner points with maximum angles
        val corners = mutableListOf<Pair<Int, Double>>()

        for (i in points.indices) {
            val prev = points[(i - 1 + points.size) % points.size]
            val curr = points[i]
            val next = points[(i + 1) % points.size]

            val angle = calculateAngle(prev, curr, next)
            corners.add(Pair(i, angle))
        }

        // Sort by angle and take 4 most prominent
        val selected = corners.sortedByDescending { it.second }
            .take(4)
            .sortedBy { it.first }
            .map { points[it.first] }

        return selected
    }

    /**
     * Calculate angle at a point
     */
    private fun calculateAngle(p1: PointF, p2: PointF, p3: PointF): Double {
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y

        val dot = v1x * v2x + v1y * v2y
        val cross = v1x * v2y - v1y * v2x

        return abs(atan2(cross.toDouble(), dot.toDouble()))
    }

    /**
     * Calculate area of polygon
     */
    private fun calculateArea(points: List<PointF>): Double {
        if (points.size < 3) return 0.0

        var area = 0.0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return abs(area / 2.0)
    }

    /**
     * Validate if quadrilateral is reasonable for a document
     */
    private fun isValidQuadrilateral(quad: List<PointF>, width: Int, height: Int): Boolean {
        if (quad.size != 4) return false

        val area = calculateArea(quad)
        val imageArea = width * height
        val areaRatio = area / imageArea

        Log.d(TAG, "Quadrilateral area ratio: $areaRatio")

        // Document should occupy at least 10% and at most 98% of image
        // More lenient than before (was 15-95%)
        return areaRatio in 0.10..0.98
    }

    /**
     * Order corners as: top-left, top-right, bottom-right, bottom-left
     */
    private fun orderCorners(points: List<PointF>): List<PointF> {
        if (points.size != 4) return points

        // Sort by y-coordinate to separate top and bottom
        val sorted = points.sortedBy { it.y }
        val top = sorted.take(2).sortedBy { it.x }
        val bottom = sorted.drop(2).sortedByDescending { it.x }

        return listOf(
            top[0],    // top-left
            top[1],    // top-right
            bottom[0], // bottom-right
            bottom[1]  // bottom-left
        )
    }

    /**
     * Calculate confidence score for detected corners
     */
    private fun calculateConfidence(corners: List<PointF>, width: Int, height: Int): Float {
        val area = calculateArea(corners)
        val imageArea = width * height
        val areaRatio = area / imageArea

        // High confidence if document occupies 25-85% of image
        return when {
            areaRatio in 0.25..0.85 -> 1.0f
            areaRatio in 0.15..0.95 -> 0.7f
            else -> 0.4f
        }
    }

    /**
     * Get default corners with padding
     */
    private fun getDefaultCorners(width: Int, height: Int): List<PointF> {
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 0.05f
        val px = w * padding
        val py = h * padding

        return listOf(
            PointF(px, py),           // top-left
            PointF(w - px, py),       // top-right
            PointF(w - px, h - py),   // bottom-right
            PointF(px, h - py)        // bottom-left
        )
    }
}
