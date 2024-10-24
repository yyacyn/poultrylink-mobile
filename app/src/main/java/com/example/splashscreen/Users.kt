package com.example.splashscreen

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Users(
    val uid: String,
    val name: String,
    val email: String,
    val password: String,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String
)

//@Serializable
//data class Users(
//    val uid: String,
//    val name: String,
//    val email: String,
//    val password: String,
//    val created_at: String,
//    val updated_at: String,
//    val deleted_at: String
//)
//
//object CustomSerializer : KSerializer<Any> {
//    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Any", PrimitiveKind.STRING)
//
//    override fun serialize(encoder: Encoder, value: Any) {
//        encoder.encodeString(value.toString())
//    }
//
//    override fun deserialize(decoder: Decoder): Any {
//        return decoder.decodeString()
//    }
//}

