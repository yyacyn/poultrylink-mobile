package com.example.splashscreen

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

data class ProductResponse(
    @SerializedName("data")
    val data: List<ProductData>
)

data class ProductData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("nama_produk")
    val nama_produk: String,
    @SerializedName("deskripsi")
    val deskripsi: String,
    @SerializedName("harga")
    val harga: String,
    @SerializedName("rating")
    val rating: String,
    @SerializedName("kategori")
    val kategori: String,
    @SerializedName("kategori_id")
    val kategori_id: String,
    @SerializedName("supplier_id")
    val supplier_id: String,
    @SerializedName("image")
    val image: String,
    @SerializedName("jumlah")
    val jumlah: String,
    @SerializedName("supplier")
    val supplier: SupplierData?,
    @SerializedName("created_at")
    val created_at: Timestamp
)
