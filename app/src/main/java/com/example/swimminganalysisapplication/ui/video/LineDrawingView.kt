package com.example.swimminganalysisapplication.ui.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

enum class DrawMode {
    FREE,
    LINE,
    CIRCLE
}

class LineDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val paths = mutableListOf<Path>()
    private var currentPath = Path()

    private var startPoint = PointF(0f, 0f)
    private var currentPoint = PointF(0f, 0f)

    private var currentDrawMode = DrawMode.FREE
    private var isDrawingEnabled = true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw all committed paths
        for (path in paths) {
            canvas.drawPath(path, paint)
        }

        // Draw the current path or preview
        when (currentDrawMode) {
            DrawMode.FREE -> canvas.drawPath(currentPath, paint)
            DrawMode.LINE -> {
                if (!startPoint.equals(0f, 0f)) {
                    canvas.drawLine(startPoint.x, startPoint.y, currentPoint.x, currentPoint.y, paint)
                }
            }
            DrawMode.CIRCLE -> {
                if (!startPoint.equals(0f, 0f)) {
                    val dx = currentPoint.x - startPoint.x
                    val dy = currentPoint.y - startPoint.y
                    val radius = sqrt((dx * dx) + (dy * dy))
                    canvas.drawCircle(startPoint.x, startPoint.y, radius, paint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) {
            return false
        }

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPoint.set(x, y)
                currentPoint.set(x, y)
                if (currentDrawMode == DrawMode.FREE) {
                    currentPath.moveTo(x, y)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentPoint.set(x, y)
                if (currentDrawMode == DrawMode.FREE) {
                    currentPath.lineTo(x, y)
                }
            }
            MotionEvent.ACTION_UP -> {
                when (currentDrawMode) {
                    DrawMode.FREE -> {
                        paths.add(Path(currentPath)) // Add a copy
                        currentPath.reset()
                    }
                    DrawMode.LINE -> {
                        val linePath = Path()
                        linePath.moveTo(startPoint.x, startPoint.y)
                        linePath.lineTo(currentPoint.x, currentPoint.y)
                        paths.add(linePath)
                    }
                    DrawMode.CIRCLE -> {
                        val dx = currentPoint.x - startPoint.x
                        val dy = currentPoint.y - startPoint.y
                        val radius = sqrt((dx * dx) + (dy * dy))
                        val circlePath = Path()
                        circlePath.addCircle(startPoint.x, startPoint.y, radius, Path.Direction.CW)
                        paths.add(circlePath)
                    }
                }
                startPoint.set(0f, 0f)
                currentPoint.set(0f, 0f)
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun setDrawMode(mode: DrawMode) {
        currentDrawMode = mode
    }

    fun clearCanvas() {
        paths.clear()
        currentPath.reset()
        invalidate()
    }

    fun setDrawingEnabled(isEnabled: Boolean) {
        isDrawingEnabled = isEnabled
    }
}