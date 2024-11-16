package com.example.splashscreen
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
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

data class CartResponse(
    @SerializedName("data")
    val data: List<CartData>,
    @SerializedName("message")
    val message: String?
)

data class CartData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("total_harga")
    val total_harga: String,
    @SerializedName("total_barang")
    val total_barang: String,
    @SerializedName("produk_id")
    val produk_id: String,
    @SerializedName("user_id")
    val user_id: String,
    @SerializedName("barang")
    val barang: ProductData,
)

data class InsertCartData(
    @SerializedName("total_barang")
    val total_barang: String,
    @SerializedName("produk_id")
    val produk_id: String,
)

data class DeleteCartRequest(
    @SerializedName("produk_id")
    val produk_id: Int,
    @SerializedName("user_id")
    val user_id: Int,
)

data class DeleteCartResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("deleted_count")
    val deleted_count: Int,
)

data class UpdateCartRequest(
    @SerializedName("id")
    val id: Int,
    @SerializedName("total_barang")
    val total_barang: Int,
)

