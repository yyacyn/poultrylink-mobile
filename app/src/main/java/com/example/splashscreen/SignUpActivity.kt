package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.format.DateTimeFormatter
import kotlin.math.log


class SignUpActivity : AppCompatActivity() {
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


    fun authSupabase(email: String, password: String, username: String) {
        lifecycleScope.launch {

            // Attempt to sign up the user using Supabase authentication
            val result = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // If sign-up is successful, get the user ID
            val auth = supabase.auth
            val session = auth.retrieveUserForCurrentSession(updateSession = true)
            val userId = session.id
            Log.d("MyTag", "User ID: $userId")

            // Now, update the user metadata to include 'display_name'
            auth.updateUser {
                data {
                    put("display_name", username)
                }
            }
            insertUser(email, password, username, userId)
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
}