package com.example.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_table")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val brand: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val imageUrl: String?,
    val ingredients: String?,
    val type: String,
    val note: String? = null,
    val expiryDate: String? = null
)
