package com.example.splashscreen

data class Cart(
    val produk_id: Long
)

data class InsertCart(
    val p_total_harga: String,
    val p_total_barang: String,
    val p_produk_id: Long,
    val p_user_id: Long
)

data class CartItem(
    val product_id: Long,
    val product_name: String,
    val product_price: String,
    val product_image: String
)


