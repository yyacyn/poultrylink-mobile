package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homepage.HomeActivity
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import retrofit2.Response

class SignInActivity : AppCompatActivity() {
    lateinit var btn_login: View
    lateinit var emailInput: EditText
    lateinit var passwordInput: EditText

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = supabase.auth.currentSessionOrNull()
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)

        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val buttonSignIn = findViewById<Button>(R.id.signin_toDashboard)
        val buttonBack = findViewById<ImageButton>(R.id.btn_back)

        buttonBack.setOnClickListener{
            val intent = Intent(this, PilihanLoginActivity::class.java)
            startActivity(intent)
        }

        buttonSignIn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (validateInput(email, password)) {
                lifecycleScope.launch {
                    try {
                        // Check if the user exists in the database before logging in
                        if (checkUserExists(email)) {
                            loginUser(email, password)
                        } else {
                            showError("Account does not exist.")
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@SignInActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        e.message?.let { it1 -> Log.d("API Error", it1) }
                    }
                }
            }
        }

        forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    // validate input
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email"
            emailInput.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            passwordInput.requestFocus()
            return false
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            passwordInput.requestFocus()
            return false
        }
        return true
    }

    private suspend fun checkUserExists(email: String): Boolean {
        val requestBody = mapOf("p_email" to email)
        val response: Response<String> = RetrofitClient.instance.getUserByEmail(requestBody)

        return when {
            response.isSuccessful -> {
                val userEmail = response.body()
                userEmail != null && userEmail.isNotEmpty() // Return true if user exists
            }
            response.errorBody() != null -> {
                // Log the error response
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("API Error", "Error: ${response.code()} - $errorMessage")
                false // If there's an error, return false
            }
            else -> {
                // Log unexpected responses
                Log.e("API Error", "Unexpected error: ${response.code()} ${response.message()}")
                false // If the request fails for any other reason, return false
            }
        }
    }

    private suspend fun loginUser(txt_email: String, txt_password: String) {
        val authResponse = supabase.auth.signInWith(Email) {
            email = txt_email
            password = txt_password
        }

        // Check Supabase Auth Result
        // Navigate to HomeActivity upon successful sign-in.
        val intent = Intent(this@SignInActivity, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


}