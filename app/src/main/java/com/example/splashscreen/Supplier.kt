package com.example.splashscreen

import com.google.gson.annotations.SerializedName


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
    val nama_toko: String,
    @SerializedName("buyer")
    val buyer: Buyer?,
)

data class SupplierResponse(
    @SerializedName("data")
    val data: List<SupplierData>
)


