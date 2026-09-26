package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val updatedAt: Long,
    val durationMs: Long,
    val previewThumbnail: String?,
    val dataJson: String
)
