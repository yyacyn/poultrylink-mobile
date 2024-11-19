package com.example.splashscreen

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: Map<String, String>
)
