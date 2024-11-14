package com.example.splashscreen

import com.google.gson.annotations.SerializedName

data class Supplier(
    val id: Long? = null,
    val alamat: String? = null,
    val kota: String? = null,
    val kodepos: String? = null,
    val provinsi: String? = null,
    val negara: String? = null,
    val deskripsi: String? = null,
    val rating: Int? = null,
    val user_id: Long? = null,
    val deleted_at: String? = null, // Adjust based on your actual schema (nullable)
    val created_at: String? = null, // Adjust based on your actual schema (nullable)
    val updated_at: String? = null, // Adjust based on your actual schema (nullable)
    val nama_toko: String? = null,
    val image: String? = null,
    val confirmed: String? = null
)

//data class SupplierData(
//    val id: Long? = null,
//    val alamat: String? = null,
//    val kota: String? = null,
//    val kodepos: String? = null,
//    val provinsi: String? = null,
//    val negara: String? = null,
//    val deskripsi: String? = null,
//    val rating: String? = null,
//    val user_id: String? = null,
//    val nama_toko: String? = null,
//)
//
//data class SupplierResponse(
//    val data: SupplierData?
//)

data class SupplierData(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("alamat")
    val alamat: String? = null,
    @SerializedName("kota")
    val kota: String? = null,
    @SerializedName("kodepos")
    val kodepos: String? = null,
    @SerializedName("provinsi")
    val provinsi: String? = null,
    @SerializedName("negara")
    val negara: String? = null,
    @SerializedName("deskripsi")
    val deskripsi: String? = null,
    @SerializedName("rating")
    val rating: String? = null,
    @SerializedName("user_id")
    val user_id: String? = null,
    @SerializedName("nama_toko")
    val nama_toko: String? = null,
)

data class SupplierResponse(
    @SerializedName("data")
    val data: List<SupplierData>
)


