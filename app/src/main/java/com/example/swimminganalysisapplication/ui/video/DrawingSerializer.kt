package com.example.swimminganalysisapplication.ui.video

import android.graphics.PointF
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive

/**
 * Serializable data classes for drawing shapes.
 */
@Serializable
data class PointData(val x: Float, val y: Float)

@Serializable
sealed class ShapeData {
    @Serializable
    data class FreeData(val points: List<PointData>) : ShapeData()

    @Serializable
    data class LineData(val start: PointData, val end: PointData) : ShapeData()

    @Serializable
    data class CircleData(val center: PointData, val radiusPoint: PointData) : ShapeData()
}

/**
 * Utility for serializing/deserializing drawing shapes to/from JSON.
 */
object DrawingSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    fun serializeShapes(shapes: List<Any>): String {
        val shapeDataList = shapes.mapNotNull { shape ->
            when (shape) {
                is LineDrawingView.Shape.Free -> {
                    val pointDataList = shape.points.map { PointData(it.x, it.y) }
                    ShapeData.FreeData(pointDataList)
                }
                is LineDrawingView.Shape.Line -> {
                    ShapeData.LineData(
                        PointData(shape.start.x, shape.start.y),
                        PointData(shape.end.x, shape.end.y)
                    )
                }
                is LineDrawingView.Shape.Circle -> {
                    ShapeData.CircleData(
                        PointData(shape.center.x, shape.center.y),
                        PointData(shape.radiusPoint.x, shape.radiusPoint.y)
                    )
                }
                else -> null
            }
        }
        return json.encodeToString(shapeDataList)
    }

    fun deserializeShapes(jsonString: String): List<LineDrawingView.Shape> {
        return try {
            val shapeDataList = json.decodeFromString<List<ShapeData>>(jsonString)
            shapeDataList.map { shapeData ->
                when (shapeData) {
                    is ShapeData.FreeData -> {
                        val points = shapeData.points.map { PointF(it.x, it.y) }.toMutableList()
                        LineDrawingView.Shape.Free(points)
                    }
                    is ShapeData.LineData -> {
                        LineDrawingView.Shape.Line(
                            PointF(shapeData.start.x, shapeData.start.y),
                            PointF(shapeData.end.x, shapeData.end.y)
                        )
                    }
                    is ShapeData.CircleData -> {
                        LineDrawingView.Shape.Circle(
                            PointF(shapeData.center.x, shapeData.center.y),
                            PointF(shapeData.radiusPoint.x, shapeData.radiusPoint.y)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Deserialize shapes for a specific video from combined JSON.
     * Combined format: {"video1":"[...]","video2":"[...]"}
     */
    fun deserializeShapesFromJson(combinedJson: String, videoKey: String): List<LineDrawingView.Shape> {
        return try {
            val jsonObject = json.parseToJsonElement(combinedJson).jsonObject
            val element = jsonObject[videoKey] ?: return emptyList()
            val videoJsonString = when (element) {
                is JsonPrimitive -> element.content
                else -> element.toString()
            }
            deserializeShapes(videoJsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
