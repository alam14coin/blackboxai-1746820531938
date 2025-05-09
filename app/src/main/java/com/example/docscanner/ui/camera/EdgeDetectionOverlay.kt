package com.example.docscanner.ui.camera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class EdgeDetectionOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cornerPoints = mutableListOf<PointF>()
    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val cornerRadius = 20f
    private var selectedCorner: Int = -1
    private var documentDetected = false

    init {
        // Initialize with default rectangle
        cornerPoints.add(PointF(0f, 0f))
        cornerPoints.add(PointF(0f, 0f))
        cornerPoints.add(PointF(0f, 0f))
        cornerPoints.add(PointF(0f, 0f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!documentDetected) return

        // Draw lines connecting corners
        for (i in cornerPoints.indices) {
            val nextIndex = (i + 1) % 4
            canvas.drawLine(
                cornerPoints[i].x,
                cornerPoints[i].y,
                cornerPoints[nextIndex].x,
                cornerPoints[nextIndex].y,
                paint
            )
        }

        // Draw corner circles
        cornerPoints.forEach { point ->
            canvas.drawCircle(point.x, point.y, cornerRadius, cornerPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedCorner = findNearestCorner(event.x, event.y)
                return selectedCorner != -1
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedCorner != -1) {
                    cornerPoints[selectedCorner].x = event.x
                    cornerPoints[selectedCorner].y = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedCorner = -1
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findNearestCorner(x: Float, y: Float): Int {
        var minDistance = Float.MAX_VALUE
        var nearestCorner = -1

        cornerPoints.forEachIndexed { index, point ->
            val distance = sqrt(
                (x - point.x).pow(2) + (y - point.y).pow(2)
            )
            if (distance < minDistance && distance < cornerRadius * 2) {
                minDistance = distance
                nearestCorner = index
            }
        }

        return nearestCorner
    }

    fun setCorners(points: List<PointF>) {
        if (points.size == 4) {
            cornerPoints.clear()
            cornerPoints.addAll(points)
            documentDetected = true
            invalidate()
        }
    }

    fun getCorners(): List<PointF> = cornerPoints.toList()

    fun setDocumentDetected(detected: Boolean) {
        documentDetected = detected
        invalidate()
    }
}
