import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class Users(
    val uid: String,
    val username: String,
    val email: String,
    val password: String,
    val created_at: String
)

// Create an instance of Users
//val instance = Users(
//    uid = "12345",
//    username = "exampleUser",
//    email = "example@example.com",
//    password = "password123",
//    created_at = "2024-10-25",
//    updated_at = "2024-10-25",
//    deleted_at = null
//)

// Serialize the instance into JSON string
//val jsonString = Json.encodeToString(Users.serializer(), instance)
//
//println(jsonString)
