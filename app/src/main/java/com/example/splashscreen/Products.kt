package com.example.splashscreen

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
    var reviews: Int? = null     // Nullable to allow updating after fetching
)

data class ProductResponse (
    val products: Array<Products>
)

