package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.homepage.HomeActivity
import com.google.firebase.Timestamp
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import kotlin.math.log
import org.mindrot.jbcrypt.BCrypt


class SignUpActivity<BitmapDrawable> : AppCompatActivity() {
    private lateinit var buttonSignUp: Button
    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var buttonBack: ImageButton

    private val supabase = createSupabaseClient(

        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)

        initializeViews()
        setupClickListeners()
        buttonSignUp.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInputs(email, name, password, confirmPassword)) {
                authSupabase(email, password, name)
            }
        }
    }

    // validate input
    private fun validateInputs(email: String, name: String, password: String, confirmPassword: String): Boolean {
        return when {
            email.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                showError("Please fill in all fields")
                false
            }
            password != confirmPassword -> {
                showError("Passwords do not match")
                false
            }
            password.length < 8 -> {
                showError("Password must be at least 8 characters")
                false
            }
            else -> true
        }
    }

    private fun initializeViews() {
        buttonSignUp = findViewById(R.id.buttonSignUp)
        emailInput = findViewById(R.id.email)
        nameInput = findViewById(R.id.name)
        passwordInput = findViewById(R.id.newPassword)
        confirmPasswordInput = findViewById(R.id.ConfirmPassword)
        buttonBack = findViewById(R.id.buttonBack)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            val intent = Intent(this, PilihanLoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun handleSignUpError(error: Exception) {
        showError("Sign up failed: ${error.message}")
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun authSupabase(email: String, password: String, username: String) {
        lifecycleScope.launch {
            try {
                // Sign up with Supabase using original password
                val result = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session = supabase.auth.retrieveUserForCurrentSession()
                val userId = session.id
                val userEmail = session.email

                // Hash password before storing in database
                val hashedPassword = hashPassword(password)
                insertUser(email, hashedPassword, username, userId)

            } catch (e: Exception) {
                handleSignUpError(e)
            }
        }
    }

    // Password hashing function using BCrypt with $2y$ prefix
    private fun hashPassword(plainTextPassword: String): String {
        // Generate salt with work factor of 12
        val salt = BCrypt.gensalt(12)
        // Hash the password with the generated salt
        var hashedPassword = BCrypt.hashpw(plainTextPassword, salt)
        // Replace $2a$ with $2y$ in the hash to match your preference
        if (hashedPassword.startsWith("$2a$")) {
            hashedPassword = "$2y$" + hashedPassword.substring(4)
        }
        return hashedPassword
    }

    // Insert user into database using supabase rest api
    fun insertUser(email: String, hashedPassword: String, username: String, userId: String) {
        val request = InsertUsers(
            p_uid = userId,
            p_username = username,
            p_email = email,
            p_password = hashedPassword  // Now storing hashed password
        )

        RetrofitClient.instance.insertUser(request).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful) {
                    val success = response.body()
                    if (success == true) {
                        Log.d("InsertUser", "User inserted successfully")
                        Toast.makeText(this@SignUpActivity, "Sign up successful", Toast.LENGTH_SHORT).show()
                        showSuccess("Sign up successful")
                        uploadDefaultAvatar(email)
                        navigateToDashboard()
                    } else {
                        Log.d("InsertUser", "User insertion failed")
                    }
                } else {
                    Log.d("InsertUser", "Server error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Log.e("InsertUser", "Network error: ${t.message}")
            }
        })
    }

    // upload user's default avatar by their email
    private fun uploadDefaultAvatar(userEmail: String) {
        lifecycleScope.launch {
            try {
                val userId = getUserIdByEmail(userEmail).toString()
                Log.d("userId", userId)
                if (userId != null) {
                    val defaultAvatar = R.drawable.fotoprofil
                    val avatarPath = "$userId/1.jpg"
                    val imageData = getDrawableAsByteArray(defaultAvatar)

                    withContext(Dispatchers.IO) {
                        Log.d("Supabase", "Attempting to upload avatar to: $avatarPath")
                        try {
                            supabase.storage.from("avatar").upload(avatarPath, imageData)
                            Log.d("Supabase", "Avatar upload successful")
                            insertBuyer(userId.toLong(), avatarPath)
                            updateUserAvatarPath(userId.toLong(), avatarPath)
                        } catch (e: Exception) {
                            Log.e("SupabaseUploadError", "Failed to upload avatar: ${e.message}")
                        }
                    }
                } else {
                    Log.e("AvatarUploadError", "User ID not found for email: $userEmail")
                }
            } catch (e: Exception) {
                Log.e("AvatarUploadError", "Error during avatar upload process: ${e.message}")
            }
        }
    }


    // converts image into bytearray to store it into supabase storage
    private fun getDrawableAsByteArray(drawableId: Int): ByteArray {
        val drawable = resources.getDrawable(drawableId, null) as android.graphics.drawable.BitmapDrawable
        val bitmap = drawable.bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private suspend fun updateUserAvatarPath(userId: Long, filePath: String) {
        supabase.postgrest["users"].update(mapOf("avatar_path" to filePath)) {
            filter { eq("id", userId) }
        }
    }

    // get user id by email to upload the avatar with user id as the folder name
    private suspend fun getUserIdByEmail(email: String): Int? {
        val requestBody = mapOf("user_email" to email)
        val response: Response<Int> = RetrofitClient.instance.getUserIdByEmail(requestBody)

        if (response.isSuccessful) {
            Log.d("APIResponse", "User ID retrieved: ${response.body()}")
            return response.body()
        } else {
            Log.e("APIError", "Failed to retrieve user ID: ${response.errorBody()?.string()}")
            return null
        }
    }

    private fun insertBuyer(userId: Long, avatarPath: String) {
        val request = InsertBuyer(
            p_user_id = userId,
            p_avatar_path = avatarPath
        )

        RetrofitClient.instance.insertBuyer(request).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful) {
                    val success = response.body()
                    if (success == true) {
                        Log.d("InsertBuyer", "Buyer inserted successfully")
                    } else {
                        Log.d("InsertBuyer", "Buyer insertion failed")
                    }
                } else {
                    Log.d("InsertBuyer", "Server error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Log.e("InsertBuyer", "Network error: ${t.message}")
            }
        })
    }
}