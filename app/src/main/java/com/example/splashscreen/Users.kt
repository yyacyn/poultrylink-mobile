package com.example.splashscreen

import kotlinx.serialization.Serializable

//@Serializable
data class Users(
    val uid: String,
    val username: String,
    val email: String,
    val password: String
)

data class InsertUsers(
    val p_email: String,
    val p_password: String,
    val p_uid: String,
    val p_username: String,
)

data class GetUserByEmail(
    val p_email: String,
)
