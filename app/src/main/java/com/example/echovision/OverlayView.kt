package com.example.echovision

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max

// This is a custom View class that will be used to draw the bounding boxes.
class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = listOf()
    private val boxPaint = Paint()
    private val textBackgroundPaint = Paint()
    private val textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = RectF()

    init {
        initPaints()
    }

    private fun initPaints() {
        // Paint for drawing text labels background.
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        // Paint for drawing text labels.
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        // Paint for drawing bounding boxes.
        boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    // This function sets the detection results and triggers a redraw.
    fun setResults(
        detectionResults: List<Detection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate() // Force the view to redraw.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw each detection on the canvas.
        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw the bounding box.
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Draw the label and confidence score.
            val drawableText = "${result.label} ${String.format("%.2f", result.confidence)}"

            // Calculate text background and position
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds.toRect())
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + 8f, // 8f is for padding
                top + textHeight + 8f,
                textBackgroundPaint
            )

            // Draw text on top of the background
            canvas.drawText(drawableText, left, top + textHeight, textPaint)
        }
    }
}
// Extension function to convert RectF to Rect
fun RectF.toRect(): android.graphics.Rect {
    return android.graphics.Rect(this.left.toInt(), this.top.toInt(), this.right.toInt(), this.bottom.toInt())
}
