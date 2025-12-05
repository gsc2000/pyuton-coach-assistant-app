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

    // Store shapes in normalized coordinates (0..1 within the video content rect)
    private sealed class Shape {
        class Free(val points: MutableList<PointF>) : Shape()
        class Line(var start: PointF, var end: PointF) : Shape()
        class Circle(var center: PointF, var radiusPoint: PointF) : Shape()
    }

    private val shapes = mutableListOf<Shape>()
    private var currentShape: Shape? = null

    private var currentDrawMode = DrawMode.FREE
    private var isDrawingEnabled = true

    // Video aspect ratio used to compute the video content rect inside this view
    private var videoAspectRatio: Float = 16f / 9f

    // Computed content rect (displayed video) in view coordinates
    private var contentLeft = 0f
    private var contentTop = 0f
    private var contentWidthF = 0f
    private var contentHeightF = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw all committed shapes by converting normalized points to display coords
        for (shape in shapes) {
            when (shape) {
                is Shape.Free -> {
                    if (shape.points.isNotEmpty()) {
                        val path = Path()
                        val p0 = toDisplay(shape.points[0])
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until shape.points.size) {
                            val pi = toDisplay(shape.points[i])
                            path.lineTo(pi.x, pi.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is Shape.Line -> {
                    val s = toDisplay(shape.start)
                    val e = toDisplay(shape.end)
                    canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                }
                is Shape.Circle -> {
                    val c = toDisplay(shape.center)
                    val rPt = toDisplay(shape.radiusPoint)
                    val dx = rPt.x - c.x
                    val dy = rPt.y - c.y
                    val radius = sqrt((dx * dx) + (dy * dy))
                    canvas.drawCircle(c.x, c.y, radius, paint)
                }
            }
        }

        // Draw preview/current shape
        currentShape?.let { shape ->
            when (shape) {
                is Shape.Free -> {
                    if (shape.points.isNotEmpty()) {
                        val path = Path()
                        val p0 = toDisplay(shape.points[0])
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until shape.points.size) {
                            val pi = toDisplay(shape.points[i])
                            path.lineTo(pi.x, pi.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is Shape.Line -> {
                    val s = toDisplay(shape.start)
                    val e = toDisplay(shape.end)
                    canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                }
                is Shape.Circle -> {
                    val c = toDisplay(shape.center)
                    val rPt = toDisplay(shape.radiusPoint)
                    val dx = rPt.x - c.x
                    val dy = rPt.y - c.y
                    val radius = sqrt((dx * dx) + (dy * dy))
                    canvas.drawCircle(c.x, c.y, radius, paint)
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
                val n = toNormalized(x, y)
                when (currentDrawMode) {
                    DrawMode.FREE -> {
                        val pts = mutableListOf<PointF>()
                        pts.add(n)
                        currentShape = Shape.Free(pts)
                    }
                    DrawMode.LINE -> {
                        currentShape = Shape.Line(n, PointF(n.x, n.y))
                    }
                    DrawMode.CIRCLE -> {
                        currentShape = Shape.Circle(PointF(n.x, n.y), PointF(n.x, n.y))
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val n = toNormalized(x, y)
                when (val cs = currentShape) {
                    is Shape.Free -> cs.points.add(n)
                    is Shape.Line -> cs.end.set(n.x, n.y)
                    is Shape.Circle -> cs.radiusPoint.set(n.x, n.y)
                    else -> {}
                }
            }
            MotionEvent.ACTION_UP -> {
                // Commit current shape (normalized points already stored)
                currentShape?.let { shapes.add(it) }
                currentShape = null
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
        shapes.clear()
        currentShape = null
        invalidate()
    }

    fun setDrawingEnabled(isEnabled: Boolean) {
        isDrawingEnabled = isEnabled
    }

    fun setVideoAspectRatio(aspect: Float?) {
        videoAspectRatio = aspect ?: (16f / 9f)
        computeContentRect(width, height)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeContentRect(w, h)
    }

    private fun computeContentRect(viewW: Int, viewH: Int) {
        if (viewW <= 0 || viewH <= 0) {
            contentLeft = 0f
            contentTop = 0f
            contentWidthF = 0f
            contentHeightF = 0f
            return
        }
        val viewAspect = viewW.toFloat() / viewH.toFloat()
        if (viewAspect > videoAspectRatio) {
            // view is wider -> fit by height
            contentHeightF = viewH.toFloat()
            contentWidthF = contentHeightF * videoAspectRatio
            contentLeft = (viewW - contentWidthF) / 2f
            contentTop = 0f
        } else {
            // view is taller -> fit by width
            contentWidthF = viewW.toFloat()
            contentHeightF = contentWidthF / videoAspectRatio
            contentLeft = 0f
            contentTop = (viewH - contentHeightF) / 2f
        }
    }

    private fun toNormalized(x: Float, y: Float): PointF {
        if (contentWidthF <= 0f || contentHeightF <= 0f) return PointF(0f, 0f)
        val nx = ((x - contentLeft) / contentWidthF).coerceIn(0f, 1f)
        val ny = ((y - contentTop) / contentHeightF).coerceIn(0f, 1f)
        return PointF(nx, ny)
    }

    private fun toDisplay(p: PointF): PointF {
        return PointF(contentLeft + p.x * contentWidthF, contentTop + p.y * contentHeightF)
    }
}