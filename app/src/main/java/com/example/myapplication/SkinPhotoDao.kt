package com.example.myapplication

import androidx.room.*

@Dao
interface SkinPhotoDao {
    @Query("SELECT * FROM skin_photos ORDER BY date DESC")
    suspend fun getAllPhotos(): List<SkinPhoto>

    @Insert
    suspend fun insertPhoto(photo: SkinPhoto)

    @Delete
    suspend fun deletePhoto(photo: SkinPhoto)
}
