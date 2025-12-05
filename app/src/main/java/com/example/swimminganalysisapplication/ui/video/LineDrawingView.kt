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

    /** Set the stroke color used for drawing shapes. */
    fun setStrokeColor(colorInt: Int) {
        paint.color = colorInt
        invalidate()
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
    
    // Selection / moving state
    private var selectedIndex: Int? = null
    private var isMoving = false
    private var lastNormalized: PointF? = null

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

        // Draw selection highlight
        selectedIndex?.let { idx ->
            if (idx in shapes.indices) {
                val selShape = shapes[idx]
                val selPaint = Paint(paint).apply {
                    color = Color.CYAN
                    strokeWidth = paint.strokeWidth + 2f
                }
                when (selShape) {
                    is Shape.Free -> {
                        if (selShape.points.isNotEmpty()) {
                            val path = Path()
                            val p0 = toDisplay(selShape.points[0])
                            path.moveTo(p0.x, p0.y)
                            for (i in 1 until selShape.points.size) {
                                val pi = toDisplay(selShape.points[i])
                                path.lineTo(pi.x, pi.y)
                            }
                            canvas.drawPath(path, selPaint)
                        }
                    }
                    is Shape.Line -> {
                        val s = toDisplay(selShape.start)
                        val e = toDisplay(selShape.end)
                        canvas.drawLine(s.x, s.y, e.x, e.y, selPaint)
                    }
                    is Shape.Circle -> {
                        val c = toDisplay(selShape.center)
                        val rPt = toDisplay(selShape.radiusPoint)
                        val dx = rPt.x - c.x
                        val dy = rPt.y - c.y
                        val radius = sqrt((dx * dx) + (dy * dy))
                        canvas.drawCircle(c.x, c.y, radius, selPaint)
                    }
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
                // If touching near an existing shape, select and start moving instead of starting a new drawing
                val hitIdx = findShapeAt(n, 0.05f)
                if (hitIdx != null) {
                    selectedIndex = hitIdx
                    isMoving = true
                    lastNormalized = n
                    return true
                }

                // Not hitting existing shape -> start creating a new one
                selectedIndex = null
                val pts = toMutableListIfFree(currentDrawMode, n)
                currentShape = pts
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val n = toNormalized(x, y)
                if (isMoving && selectedIndex != null) {
                    val idx = selectedIndex!!
                    val last = lastNormalized ?: n
                    val dx = n.x - last.x
                    val dy = n.y - last.y
                    moveShapeBy(idx, dx, dy)
                    lastNormalized = n
                } else {
                    when (val cs = currentShape) {
                        is Shape.Free -> cs.points.add(n)
                        is Shape.Line -> cs.end.set(n.x, n.y)
                        is Shape.Circle -> cs.radiusPoint.set(n.x, n.y)
                        else -> {}
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isMoving) {
                    isMoving = false
                    lastNormalized = null
                } else {
                    // Commit current shape (normalized points already stored)
                    currentShape?.let { shapes.add(it) }
                    currentShape = null
                }
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

    // Find index of a shape near normalized point n within threshold (normalized coordinates)
    private fun findShapeAt(n: PointF, threshold: Float): Int? {
        // check shapes in reverse order so topmost (last drawn) is hit first
        for (i in shapes.indices.reversed()) {
            val s = shapes[i]
            when (s) {
                is Shape.Free -> {
                    for (pt in s.points) {
                        val dx = pt.x - n.x
                        val dy = pt.y - n.y
                        if (sqrt(dx * dx + dy * dy) <= threshold) return i
                    }
                }
                is Shape.Line -> {
                    val d = pointToSegmentDistance(n, s.start, s.end)
                    if (d <= threshold) return i
                }
                is Shape.Circle -> {
                    val dx = n.x - s.center.x
                    val dy = n.y - s.center.y
                    val dist = sqrt(dx * dx + dy * dy)
                    // consider near circumference or inside
                    val rdx = n.x - s.radiusPoint.x
                    val rdy = n.y - s.radiusPoint.y
                    val rdist = sqrt(rdx * rdx + rdy * rdy)
                    val r = sqrt((s.radiusPoint.x - s.center.x) * (s.radiusPoint.x - s.center.x) + (s.radiusPoint.y - s.center.y) * (s.radiusPoint.y - s.center.y))
                    if (abs(dist - r) <= threshold || dist <= r + threshold) return i
                }
            }
        }
        return null
    }

    private fun abs(v: Float): Float = if (v < 0) -v else v

    private fun pointToSegmentDistance(p: PointF, v: PointF, w: PointF): Float {
        // compute distance from p to segment vw (all in normalized coords)
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return sqrt((p.x - v.x) * (p.x - v.x) + (p.y - v.y) * (p.y - v.y))
        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
        if (t < 0f) t = 0f
        else if (t > 1f) t = 1f
        val projx = v.x + t * (w.x - v.x)
        val projy = v.y + t * (w.y - v.y)
        return sqrt((p.x - projx) * (p.x - projx) + (p.y - projy) * (p.y - projy))
    }

    private fun toMutableListIfFree(mode: DrawMode, n: PointF): Shape {
        return when (mode) {
            DrawMode.FREE -> Shape.Free(mutableListOf(n))
            DrawMode.LINE -> Shape.Line(PointF(n.x, n.y), PointF(n.x, n.y))
            DrawMode.CIRCLE -> Shape.Circle(PointF(n.x, n.y), PointF(n.x, n.y))
        }
    }

    private fun moveShapeBy(idx: Int, dx: Float, dy: Float) {
        if (idx !in shapes.indices) return
        val s = shapes[idx]
        when (s) {
            is Shape.Free -> {
                for (pt in s.points) {
                    pt.x = (pt.x + dx).coerceIn(0f, 1f)
                    pt.y = (pt.y + dy).coerceIn(0f, 1f)
                }
            }
            is Shape.Line -> {
                s.start.x = (s.start.x + dx).coerceIn(0f, 1f)
                s.start.y = (s.start.y + dy).coerceIn(0f, 1f)
                s.end.x = (s.end.x + dx).coerceIn(0f, 1f)
                s.end.y = (s.end.y + dy).coerceIn(0f, 1f)
            }
            is Shape.Circle -> {
                s.center.x = (s.center.x + dx).coerceIn(0f, 1f)
                s.center.y = (s.center.y + dy).coerceIn(0f, 1f)
                s.radiusPoint.x = (s.radiusPoint.x + dx).coerceIn(0f, 1f)
                s.radiusPoint.y = (s.radiusPoint.y + dy).coerceIn(0f, 1f)
            }
        }
        invalidate()
    }

    fun deleteSelectedShape() {
        selectedIndex?.let { idx ->
            if (idx in shapes.indices) {
                shapes.removeAt(idx)
                selectedIndex = null
                invalidate()
            }
        }
    }
}