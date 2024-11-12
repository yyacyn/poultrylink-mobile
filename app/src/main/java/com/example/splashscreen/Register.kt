package com.example.splashscreen

import kotlinx.serialization.Serializable


@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val data: RegisterData?
)

@Serializable
data class RegisterData(
    val token: String,
    val username: String,
    val user_id: Long
)

@Serializable
data class User(
    val id: Int,
    val username: String
)

@Serializable
data class InsertUser(
    val email: String,
    val username: String,
    val password: String,
    val confirm_password: String
)

