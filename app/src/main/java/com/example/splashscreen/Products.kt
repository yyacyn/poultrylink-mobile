package com.example.splashscreen

data class Products(
    val id: Long,
    val nama_produk: String,
    val deskripsi: String,
    val kategori_id: Long,
    val supplier_id: Long,
    val rating: Float,
    val reviews: Int,
    val harga: Long,
    val image: String
)

data class ProductResponse (
    val products: Array<Products>
)

