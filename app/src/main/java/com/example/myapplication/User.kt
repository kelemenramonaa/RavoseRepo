package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users_table")
data class User(
    @PrimaryKey val email: String,
    val surname: String,
    val firstName: String,
    val nickname: String?,
    val password: String,
    val displayName: String,
    val skinType: String = "Zsíros",
    val morningNotif: Boolean = true,
    val eveningNotif: Boolean = true
)
