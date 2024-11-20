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
import androidx.lifecycle.lifecycleScope
import com.example.homepage.HomeActivity
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import org.mindrot.jbcrypt.BCrypt

class SignUpActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Storage)
    }

    private lateinit var buttonSignUp: Button
    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var buttonBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)

        initializeViews()
        setupClickListeners()

        buttonSignUp.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val username = nameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInputs(email, username, password, confirmPassword)) {
                registerUser(email, username, password, confirmPassword)
            }
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
            startActivity(Intent(this, PilihanLoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(email: String, username: String, password: String, confirmPassword: String): Boolean {
        return when {
            email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                showToast("Please fill in all fields")
                false
            }
            password != confirmPassword -> {
                showToast("Passwords do not match")
                false
            }
            password.length < 8 -> {
                showToast("Password must be at least 8 characters")
                false
            }
            else -> true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun registerUser(email: String, username: String, password: String, confirmPassword: String) {

        val request = InsertUser(
            email = email,
            username = username,
            password = password,
            confirm_password = password
        )

        RetrofitClient.instance.registerUser(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { registerResponse ->
                        Log.d("registerresponse", "Response: $registerResponse")
                        if (registerResponse.success) {
                            val userId = registerResponse.data?.user_id
                            val token = registerResponse.data?.token
                            if (userId != null) {
                                if (token != null) {
                                    storeToken(token)
                                }
                                uploadDefaultAvatar(userId.toString())
                                createBuyerProfile(userId.toString())
                            }
                        } else {
                            showToast("Sign up failed: ${registerResponse.message}")
                        }
                    } ?: run {
                        Log.e("registerresponse", "Response body is null")
                        showToast("Sign up failed: Empty response")
                    }
                } else {
                    Log.e("registerresponse", "Server error: ${response.errorBody()?.string()}")
                    showToast("Server error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("registerresponse", "Network error: ${t.message}")
                showToast("Network error: ${t.message}")
            }
        })
    }

    private fun createBuyerProfile(userId: String) {
        val token = "Bearer ${getStoredToken()}"
        Log.d("tokenforbuyer", token)
        if (token != null) {
            val buyerProfileRequest = BuyerProfileRequest(
//                user_id = userId.toLong(),
                default_avatar = userId
            )

            val request = RetrofitClient.instance.createBuyerProfile(token, buyerProfileRequest)

            request.enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {
                        Log.d("Buyer", "Buyer profile created successfully")
                        showToast("Sign up successful")
                        navigateToHome(token)
                    } else {
                        Log.e("Buyer", "Failed to create buyer profile: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                    Log.e("Buyer", "Error creating buyer profile: ${t.message}")
                }
            })
        } else {
            Log.e("Auth", "No token found in SharedPreferences")
            showToast("Error: No token available")
        }
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun navigateToHome(token: String) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("TOKEN", token)
        startActivity(intent)
        finish()
    }

    private fun storeToken(token: String) {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("TOKEN", token)
        editor.apply()
    }


    private fun uploadDefaultAvatar(userId: String) {
        lifecycleScope.launch {
            try {
                val avatarPath = "$userId/1.jpg"
                val imageData = getDrawableAsByteArray(R.drawable.fotoprofil)

                withContext(Dispatchers.IO) {
                    try {
                        supabase.storage.from("avatar").upload(avatarPath, imageData)
                        Log.d("Supabase", "Avatar upload successful")
                    } catch (e: Exception) {
                        Log.e("SupabaseUploadError", "Failed to upload avatar: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AvatarUploadError", "Error during avatar upload process: ${e.message}")
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

}
