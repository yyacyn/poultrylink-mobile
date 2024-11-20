package com.example.splashscreen

import android.net.Uri
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.io.File

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

@Serializable
data class InsertBuyer(
    val p_user_id: Long,
    val p_avatar_path: String
)

data class UpdateProfileRequest(
    @SerializedName("firstname")
    val firstname: String,
    @SerializedName("lastname")
    val lastname: String,
    @SerializedName("alamat")
    val alamat: String,
    @SerializedName("telepon")
    val telepon: String,
    @SerializedName("kota")
    val kota: String,
    @SerializedName("kodepos")
    val kodepos: String,
    @SerializedName("provinsi")
    val provinsi: String,
    @SerializedName("negara")
    val negara: String,
)

data class UpdateProfileResponse(
    val success: Boolean,
    val message: String
)

data class UserProfile(
    val firstName: String? = null,
    val lastName: String? = null,
    val address: String? = null,
    val phoneNumber: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val province: String? = null,
    val country: String? = null,
    val avatar_path: Uri? = null
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




