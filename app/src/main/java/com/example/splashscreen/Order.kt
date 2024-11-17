package com.example.splashscreen

import com.google.gson.annotations.SerializedName

data class OrderResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("order")
    val order: OrderData, // Changed from List<OrderData> to a single object
    @SerializedName("no_tagihan")
    val noTagihan: String // Correct field name
)

data class OrderData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("user_id")
    val userId: Long, // Changed type from String to Long
    @SerializedName("total")
    val total: Int, // Changed type from String to Int
    @SerializedName("harga")
    val harga: Long, // Changed type from String to Long
    @SerializedName("confirmed")
    val confirmed: String,
    @SerializedName("metode_pembayaran")
    val metodePembayaran: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class InsertOrder(
    @SerializedName("cart_id")
    val cart_id: List<Long>,
    @SerializedName("metode_pembayaran")
    val metode_pembayaran: String,
)

data class OrderDetailResponse(
    @SerializedName("data")
    val data: List<OrderDetailData>,
)

data class OrderDetailData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("order_id")
    val order_id: Long,
    @SerializedName("tanggal")
    val tanggal: String,
    @SerializedName("produk")
    val produk: String,
    @SerializedName("produk_image")
    val produk_image: String,
    @SerializedName("produk_kategori")
    val produk_kategori: String,
    @SerializedName("quantity")
    val quantity: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("total_price")
    val total_price: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("supplier")
    val supplier: String,
    @SerializedName("order_deleted")
    val order_deleted: String?,
    @SerializedName("buyer")
    val buyer: BuyerData
)

data class RetrieveOrderRequest(
    @SerializedName("id")
    val id: Int,
)

data class CancelOrderRequest(
    @SerializedName("order_id")
    val order_id: Int,
    @SerializedName("produk_id")
    val produk_id: Int,
)

data class CancelOrderResponse(
    @SerializedName("message")
    val message: String?,
    @SerializedName("order")
    val order: OrderData?,
)