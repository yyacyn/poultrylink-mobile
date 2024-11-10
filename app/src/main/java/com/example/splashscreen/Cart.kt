package com.example.splashscreen
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

data class Cart(
    val produk_id: Long
)

data class InsertCart(
    val p_total_harga: String,
    val p_total_barang: String,
    val p_produk_id: Long,
    val p_user_id: Long
)

@Parcelize
data class CartItem(
    val productId: Long,
    val productName: String,
    val productImage: String,
    val productKategori: String,
    val quantity: Int,
    val itemPrice: Long,
    val totalPrice: Long
) : Parcelable

@Serializable
data class CartUpdateRequest(
    val p_cart_id: Long,
    val p_quantity: Int
)
