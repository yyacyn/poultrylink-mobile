package com.example.splashscreen

import kotlinx.serialization.Serializable

@Serializable
data class Users(
    val id: String? = null,
    val uid: String,
    val username: String,
    val email: String,
    val password: String
)


