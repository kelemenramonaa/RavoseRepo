package com.example.myapplication

import androidx.room.*

@Dao
interface SkinPhotoDao {
    @Query("SELECT * FROM skin_photos WHERE userEmail = :email ORDER BY date DESC")
    suspend fun getAllPhotos(email: String): List<SkinPhoto>

    @Insert
    suspend fun insertPhoto(photo: SkinPhoto)

    @Delete
    suspend fun deletePhoto(photo: SkinPhoto)
}
