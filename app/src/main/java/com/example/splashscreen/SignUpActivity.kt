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

            // Call validateInputs function, and if true, proceed with signup
            if (validateInputs(email, name, password, confirmPassword)) {
                authSupabase(email, password, name)
            }
        }
    }

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
                val result = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session = supabase.auth.retrieveUserForCurrentSession()
                val userId = session.id
                val userEmail = session.email
                insertUser(email, password, username, userId)

                // Upload default profile picture
                uploadDefaultAvatar(userEmail.toString())
            } catch (e: Exception) {
                handleSignUpError(e)
            }
        }
    }

    fun insertUser(email: String, password: String, username: String, userId: String) {

        val request = InsertUsers(
            p_uid =  userId,
            p_username = username,
            p_email = email,
            p_password = password
        )

        RetrofitClient.instance.insertUser(request).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful) {
                    val success = response.body()
                    if (success == true) {
                        Log.d("InsertUser", "User inserted successfully")
                        Toast.makeText(this@SignUpActivity, "Sign up successful", Toast.LENGTH_SHORT).show()
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

    private fun uploadDefaultAvatar(userEmail: String) {
        lifecycleScope.launch {
            val userId = getUserIdByEmail(userEmail)
            if (userId != null) {
                val defaultAvatar = R.drawable.fotoprofil  // Your drawable resource
                val avatarPath = "avatars/$userId/avatar.jpg"  // Use user ID for folder
                val imageData = getDrawableAsByteArray(defaultAvatar)

                withContext(Dispatchers.IO) {
                    try {
                        supabase.storage.from("avatar").upload(avatarPath, imageData)
                        updateUserAvatarPath(userEmail, avatarPath)
                        Log.d("Supabase", "Default avatar uploaded: $avatarPath")
                    } catch (e: Exception) {
                        Log.e("SupabaseUploadError", "Failed to upload avatar: ${e.message}")
                    }
                }
            } else {
                Log.e("AvatarUploadError", "User ID not found for email: $userEmail")
            }
        }
    }


    private fun getDrawableAsByteArray(drawableId: Int): ByteArray {
        val drawable = resources.getDrawable(drawableId, null) as android.graphics.drawable.BitmapDrawable
        val bitmap = drawable.bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private suspend fun updateUserAvatarPath(email: String, filePath: String) {
        supabase.postgrest["users"].update(mapOf("avatar_path" to filePath)) {
            filter { eq("email", email) }
        }
    }

    private fun uploadAndDisplayAvatar(filename: String, drawableId: Int) {
        lifecycleScope.launch {
            val currentEmail = supabase.auth.retrieveUserForCurrentSession().email ?: return@launch
            val avatarPath = "avatars/$currentEmail/$filename"

            try {
                // Switch to background thread for network operations
                withContext(Dispatchers.IO) {
                    // Check if the user already has an avatar
                    val existingFiles = supabase.storage.from("avatar").list("avatars/$currentEmail")
                    val imageData = getDrawableAsByteArray(drawableId)

                    if (existingFiles.isNotEmpty()) {
                        // Update the existing file
                        supabase.storage.from("avatar").update(avatarPath, imageData)
                        Log.d("Supabase", "Existing avatar updated: $avatarPath")
                    } else {
                        // Upload a new avatar
                        supabase.storage.from("avatar").upload(avatarPath, imageData)
                        Log.d("Supabase", "New avatar uploaded: $avatarPath")
                    }

                    // Update avatar path in user's database record
                    updateUserAvatarPath(currentEmail, avatarPath)
                }

                // Load the image from Supabase Storage URL into ImageView with Glide
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$avatarPath"
                Glide.with(this@SignUpActivity)
                    .load(imageUrl)
                    .skipMemoryCache(true) // skip memory cache
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // skip disk cache
                    .into(findViewById(R.id.user_pfp))
                Log.d("Supabase", "Avatar displayed successfully")

            } catch (e: Exception) {
                Log.e("SupabaseUploadError", "Failed to upload or display avatar: ${e.message}")
            }
        }
    }

    private suspend fun getUserIdByEmail(email: String): Int? {
        val requestBody = mapOf("p_email" to email)
        val response: Response<Int> = RetrofitClient.instance.getUserIdByEmail(requestBody)

        if (response.isSuccessful) {
            // Log the raw response to see if you are receiving the correct ID
            Log.d("APIResponse", "User ID retrieved: ${response.body()}")
            return response.body() // This will be the user ID directly
        } else {
            Log.e("APIError", "Failed to retrieve user ID: ${response.errorBody()?.string()}")
            return null
        }
    }
}