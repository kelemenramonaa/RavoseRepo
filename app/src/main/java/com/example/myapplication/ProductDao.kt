package com.example.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products_table ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products_table ORDER BY id DESC")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT * FROM products_table WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("""
        SELECT * FROM products_table 
        WHERE (:category IS NULL OR type LIKE '%' || :category || '%')
        AND (:query = '' OR name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%')
        ORDER BY id DESC
    """)
    fun searchProductsCombined(query: String, category: String?): Flow<List<Product>>
}
