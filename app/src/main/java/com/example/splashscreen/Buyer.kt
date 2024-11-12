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

data class BuyerResponse(
    val data: BuyerData
)


data class BuyerData(
    val id: Long?,
    val firstname: String?,
    val lastname: String?,
    val alamat: String?,
    val telepon: String?,
    val kota: String?,
    val kodepos: String?,
    val provinsi: String?,
    val negara: String?,
    val user_id: Long?,
    val avatar_path: String?,
    val default_avatar: String?,
    val user: User,
    val ulasan: List<Ulasan>
)

//@Serializable
data class BuyerProfileRequest(
    val user_id: Long? = null,
//    val firstname: String? = null,
//    val lastname: String? = null,
//    val alamat: String? = null,
//    val telepon: String? = null,
//    val kota: String? = null,
//    val kodepos: String? = null,
//    val provinsi: String? = null,
//    val negara: String? = null,
    val default_avatar: String? = null,
//    val avatar_path: String? = null
)




