package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode.Callback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignInActivity : AppCompatActivity() {
    lateinit var btn_login: View
    lateinit var emailInput: EditText
    lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)

//        val btn_login = findViewById<View>(R.id.btn_login)
//        val emailInput = findViewById<EditText>(R.id.emailInput)
//        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)

        forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}

