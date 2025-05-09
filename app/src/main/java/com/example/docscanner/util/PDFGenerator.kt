package com.example.docscanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.print.PrintAttributes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PDFGenerator {
    companion object {
        fun createPDF(
            context: Context,
            images: List<Bitmap>,
            outputPath: String,
            pageSize: PrintAttributes.MediaSize = PrintAttributes.MediaSize.ISO_A4,
            quality: Int = 100
        ): Boolean {
            try {
                val pdfDocument = PdfDocument()
                
                // Calculate page dimensions based on media size
                val pageWidth = pageSize.widthMils * 72 / 1000 // Convert mils to points
                val pageHeight = pageSize.heightMils * 72 / 1000
                
                images.forEachIndexed { index, bitmap ->
                    // Scale bitmap to fit page while maintaining aspect ratio
                    val scaledBitmap = scaleBitmapToFitPage(bitmap, pageWidth, pageHeight)
                    
                    // Center bitmap on page
                    val xOffset = (pageWidth - scaledBitmap.width) / 2f
                    val yOffset = (pageHeight - scaledBitmap.height) / 2f
                    
                    // Create page info
                    val pageInfo = PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), index + 1).create()
                    
                    // Start a new page
                    val page = pdfDocument.startPage(pageInfo)
                    
                    // Draw bitmap on page
                    page.canvas.drawBitmap(scaledBitmap, xOffset, yOffset, null)
                    
                    // Finish page
                    pdfDocument.finishPage(page)
                    
                    // Recycle scaled bitmap
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                }
                
                // Write the PDF file
                val outputFile = File(outputPath)
                FileOutputStream(outputFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                
                // Close the document
                pdfDocument.close()
                
                return true
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        }
        
        private fun scaleBitmapToFitPage(
            bitmap: Bitmap,
            pageWidth: Float,
            pageHeight: Float
        ): Bitmap {
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            
            // Calculate scaling factors for width and height
            val scaleWidth = pageWidth / imageWidth
            val scaleHeight = pageHeight / imageHeight
            
            // Use the smaller scaling factor to ensure the image fits on the page
            val scaleFactor = minOf(scaleWidth, scaleHeight)
            
            // Calculate new dimensions
            val newWidth = (imageWidth * scaleFactor).toInt()
            val newHeight = (imageHeight * scaleFactor).toInt()
            
            // Return original bitmap if no scaling is needed
            if (newWidth == imageWidth && newHeight == imageHeight) {
                return bitmap
            }
            
            // Create and return scaled bitmap
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }
}
