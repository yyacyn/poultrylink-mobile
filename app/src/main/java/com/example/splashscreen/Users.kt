import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Users(
    val uid: String,
    val username: String,
    val email: String,
    val password: String,
)

fun main() {
    val user = Users(uid = "123", username = "JohnDoe", email = "john@example.com", password = "securePassword")
    val jsonString = Json.encodeToString(user)
    println(jsonString) // Output: {"uid":"123","username":"JohnDoe","email":"john@example.com","password":"securePassword"}
}