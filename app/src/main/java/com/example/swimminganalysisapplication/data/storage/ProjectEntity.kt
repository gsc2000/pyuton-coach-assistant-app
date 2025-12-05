package com.example.swimminganalysisapplication.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity for storing analysis projects with video URIs and drawing data.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val videoUri1: String?,
    val videoUri2: String?,
    val startPosition1Ms: Long,
    val startPosition2Ms: Long,
    val drawingsJson: String, // JSON serialized drawings
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
