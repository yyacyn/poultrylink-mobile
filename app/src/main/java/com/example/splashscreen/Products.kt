package com.example.splashscreen

import com.google.gson.annotations.SerializedName

data class Products(
    val id: Long,
    val nama_produk: String,
    val deskripsi: String,
    val kategori_id: Long,
    val supplier_id: Long,
    val harga: Long,
    val ulasan: Long? = null, // This field represents the count of total reviews, if applicable
    val image: String,
    var rating: Double? = null,  // Nullable to allow updating after fetching
    var reviews: Int? = null,
    val jumlah: Long? = null// Nullable to allow updating after fetching
)


data class ProductResponse(
    @SerializedName("data")
    val data: List<ProductData>
)

data class ProductData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nama_produk")
    val nama_produk: String,
    @SerializedName("deskripsi")
    val deskripsi: String,
    @SerializedName("harga")
    val harga: String,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("kategori_id")
    val kategori_id: String,
    @SerializedName("supplier_id")
    val supplier_id: String,
    @SerializedName("image")
    val image: String
)
