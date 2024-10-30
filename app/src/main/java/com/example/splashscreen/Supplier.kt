package com.example.splashscreen

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
