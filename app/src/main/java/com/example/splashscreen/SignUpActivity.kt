package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.homepage.HomeActivity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.log

private val supabase = createSupabaseClient(
    supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
) {
    install(Auth)
    install(Postgrest)
}

class SignUpActivity : AppCompatActivity() {
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
            val name = nameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Call validateInputs function, and if true, proceed with signup
            if (validateInputs(email, name, password, confirmPassword)) {
                performSignUp(email, password, name)
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
        buttonBack.setOnClickListener { /* Navigate back */ }
    }


    private fun performSignUp(email: String, password: String, name: String) {
        lifecycleScope.launch {
            try {
                // Attempt to sign up the user
                val result = supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // If sign-up is successful, get the user ID
                val auth = supabase.auth

                val userId = auth.retrieveUserForCurrentSession(updateSession = true).id
                Log.d("MyTag", userId)

                // Insert user details into the "users" table in Supabase
                val userDetails = mapOf(
                    "uid" to userId,
                    "name" to name,
                    "email" to email,
                    "password" to password
                )

                supabase.postgrest["users"]
                    .insert(userDetails)

            } catch (e: Exception) {
                handleSignUpError(e)
            }
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

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}