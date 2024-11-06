package com.example.splashscreen

import kotlinx.serialization.Serializable

// Data class representing a buyer record
@Serializable
data class Buyer(
    val id: Long,
    val alamat: String,
    val telepon: Long,
    val kota: String,
    val kodepos: String,
    val provinsi: String,
    val negara: String,
    val user_id: Long,
    val created_at: String,
    val updated_at: String,
    val firstname: String,
    val lastname: String,
)

// Request data class for inserting a buyer
@Serializable
data class InsertBuyer(
    val p_user_id: Long,
    val p_avatar_path: String
)

@Serializable
data class UpdateBuyer(
    val p_uid: Long,
    val p_alamat: String,
    val p_telepon: Long,
    val p_kota: String,
    val p_kodepos: String,
    val p_provinsi: String,
    val p_negara: String,
    val p_firstname: String,
    val p_lastname: String,
)

@Serializable
data class BuyerDetails(
    val user_id: Long,
    val firstname: String?,
    val lastname: String?,
    val alamat: String?,
    val telepon: Long?,
    val kota: String?,
    val kodepos: String?,
    val provinsi: String?,
    val negara: String?
)

