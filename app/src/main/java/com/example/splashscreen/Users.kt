package com.example.splashscreen

import kotlinx.serialization.Serializable

@Serializable
data class Users(
    val uid: String,
    val name: String,
    val email: String,
    val password: String
)
