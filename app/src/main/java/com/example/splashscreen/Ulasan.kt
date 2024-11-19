package com.example.splashscreen

import com.google.gson.annotations.SerializedName

data class Ulasan(
    val id: Long? = null,
    val ulasan: String? = null,
    val product_id: Long? = null,
    val user_id: Long? = null,
    val rating: Long? = null,
)

data class ReviewResponse(
    @SerializedName("data")
    val data: List<ReviewData>,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
)

data class PostReviewResponse(
    @SerializedName("data")
    val data: ReviewData,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
)

data class ReviewData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("produk_id")
    val produk_id: String,
    @SerializedName("user_id")
    val user_id: String,
    @SerializedName("ulasan")
    val ulasan: String,
    @SerializedName("created_at")
    val created_at: String,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("user")
    val user: UlasanUser,
    @SerializedName("buyer")
    val buyer: UlasanBuyer
)

data class UlasanUser(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String
)

data class UlasanBuyer(
    @SerializedName("id")
    val id: Long,
    @SerializedName("avatar_path")
    val avatar_path: String
)

data class ReviewPostRequest(
    @SerializedName("produk_id")
    val produk_id: Long,
    @SerializedName("ulasan")
    val ulasan: String,
    @SerializedName("rating")
    val rating: Int,
)