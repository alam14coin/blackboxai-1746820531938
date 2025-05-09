package com.example.docscanner.util

import android.graphics.Bitmap
import android.graphics.PointF
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

class ImageProcessor {
    
    fun applyBlackAndWhite(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        
        // Apply adaptive threshold
        Imgproc.adaptiveThreshold(
            mat,
            mat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        Utils.matToBitmap(mat, result)
        mat.release()
        
        return result
    }
    
    fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        Utils.matToBitmap(mat, result)
        mat.release()
        
        return result
    }
    
    fun enhanceDocument(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        
        // Apply bilateral filter to smooth while preserving edges
        Imgproc.bilateralFilter(mat, mat, 9, 75.0, 75.0)
        
        // Enhance contrast using CLAHE (Contrast Limited Adaptive Histogram Equalization)
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(mat, mat)
        
        // Convert back to color
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGR)
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        Utils.matToBitmap(mat, result)
        mat.release()
        
        return result
    }
    
    fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val center = Point(mat.cols() / 2.0, mat.rows() / 2.0)
        val rotationMatrix = Imgproc.getRotationMatrix2D(center, degrees.toDouble(), 1.0)
        
        Imgproc.warpAffine(
            mat,
            mat,
            rotationMatrix,
            mat.size(),
            Imgproc.INTER_LINEAR
        )
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        Utils.matToBitmap(mat, result)
        mat.release()
        
        return result
    }
    
    fun cropPerspective(bitmap: Bitmap, corners: List<PointF>): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert corners to OpenCV points
        val sourcePoints = MatOfPoint2f().apply {
            fromArray(
                Point(corners[0].x.toDouble(), corners[0].y.toDouble()),
                Point(corners[1].x.toDouble(), corners[1].y.toDouble()),
                Point(corners[2].x.toDouble(), corners[2].y.toDouble()),
                Point(corners[3].x.toDouble(), corners[3].y.toDouble())
            )
        }
        
        // Calculate output size
        val width = max(
            distance(corners[0], corners[1]),
            distance(corners[2], corners[3])
        ).toInt()
        
        val height = max(
            distance(corners[0], corners[3]),
            distance(corners[1], corners[2])
        ).toInt()
        
        // Define destination points
        val destinationPoints = MatOfPoint2f().apply {
            fromArray(
                Point(0.0, 0.0),
                Point(width.toDouble(), 0.0),
                Point(width.toDouble(), height.toDouble()),
                Point(0.0, height.toDouble())
            )
        }
        
        // Get perspective transform
        val perspectiveTransform = Imgproc.getPerspectiveTransform(sourcePoints, destinationPoints)
        
        // Apply perspective transform
        val output = Mat()
        Imgproc.warpPerspective(
            mat,
            output,
            perspectiveTransform,
            Size(width.toDouble(), height.toDouble())
        )
        
        val result = Bitmap.createBitmap(width, height, bitmap.config)
        Utils.matToBitmap(output, result)
        
        mat.release()
        output.release()
        sourcePoints.release()
        destinationPoints.release()
        perspectiveTransform.release()
        
        return result
    }
    
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    fun detectDocumentEdges(bitmap: Bitmap): List<PointF>? {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        
        // Apply Gaussian blur
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        
        // Apply Canny edge detection
        val edges = Mat()
        Imgproc.Canny(gray, edges, 75.0, 200.0)
        
        // Find contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Find the largest contour that could be a document
        var maxArea = 0.0
        var documentContour: MatOfPoint? = null
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(
                    MatOfPoint2f(*contour.toArray()),
                    approx,
                    0.02 * perimeter,
                    true
                )
                
                if (approx.total() == 4L) {
                    maxArea = area
                    documentContour = MatOfPoint(*approx.toArray())
                }
            }
        }
        
        // Clean up
        mat.release()
        gray.release()
        edges.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        // Convert detected corners to PointF list
        return documentContour?.toList()?.map { point ->
            PointF(point.x.toFloat(), point.y.toFloat())
        }?.takeIf { it.size == 4 }
    }
}
