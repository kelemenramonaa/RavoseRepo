package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skin_photos")
data class SkinPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val date: Long,
    val note: String? = null,
    val userEmail: String
)
