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
import kotlinx.coroutines.launch
import retrofit2.Response

class SignInActivity : AppCompatActivity() {
    lateinit var btn_login: View
    lateinit var emailInput: EditText
    lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)

        val buttonSignIn = findViewById<Button>(R.id.signin_toDashboard)
        val buttonBack = findViewById<ImageButton>(R.id.btn_back)

        buttonBack.setOnClickListener {
            val intent = Intent(this, PilihanLoginActivity::class.java)
            startActivity(intent)
        }

        buttonSignIn.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (validateInput(email, password)) {
                lifecycleScope.launch {
                    loginUser(email, password)
                }
            }
        }

//        forgotPassword.setOnClickListener {
//            val intent = Intent(this, ForgotPasswordActivity::class.java)
//            startActivity(intent)
//        }
    }

    // Validate input
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

        return true
    }

    // Login user using the Laravel API
    private suspend fun loginUser(email: String, password: String) {
        try {
            val response = RetrofitClient.instance.loginUser(mapOf("email" to email, "password" to password))

            if (response.isSuccessful && response.body()?.success == true) {
                val token = response.body()?.data?.get("token")
                Log.d("response", "${response.body()}")
                Log.d("tokenuser", "$token")
                if (token != null) {
                    storeToken(token)
                    // Store token and proceed to home
                    navigateToHome(token)
                } else {
                    showError("Token missing in response.")
                }
            } else {
                showError("Login failed: ${response.body()?.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            showError("Login failed: ${e.message}")
            Log.d("API Error", e.message ?: "Unknown error")
        }
    }

    // Show error message
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Navigate to home activity after successful login
    private fun navigateToHome(token: String) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("TOKEN", token)
        startActivity(intent)
        finish()
    }

    // Store the token in SharedPreferences
    private fun storeToken(token: String) {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("TOKEN", token)
        editor.apply()
    }

}
