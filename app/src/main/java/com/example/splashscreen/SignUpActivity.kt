package com.example.splashscreen

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.homepage.HomeActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.core.Tag
import com.google.firebase.firestore.FirebaseFirestore


class SignUpActivity : AppCompatActivity() {
    private lateinit var buttonSignUp: Button
    private lateinit var emailInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var buttonBack: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var store: FirebaseFirestore

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        buttonSignUp = findViewById(R.id.buttonSignUp)
        emailInput = findViewById(R.id.email)
        nameInput = findViewById(R.id.name)
        passwordInput = findViewById(R.id.newPassword)
        confirmPasswordInput = findViewById(R.id.ConfirmPassword)
        buttonBack = findViewById(R.id.buttonBack)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        store = FirebaseFirestore.getInstance()

        buttonBack.setOnClickListener {
            val intent = Intent(this, PilihanLoginActivity::class.java)
            startActivity(intent)
        }

        buttonSignUp.setOnClickListener {
            val textemail = emailInput.text.toString()
            val textname = nameInput.text.toString()
            val textpassword = passwordInput.text.toString()
            val textconfirmPassword = confirmPasswordInput.text.toString()

            if (TextUtils.isEmpty(textemail) || TextUtils.isEmpty(textname) || TextUtils.isEmpty(textpassword) || TextUtils.isEmpty(textconfirmPassword)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (
                textpassword != textconfirmPassword
            ) {
                Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
            } else if (textpassword.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(textname, textemail, textpassword)

            }

        }
    }


    data class ReadWriteUserDetails(
        val name: String? = null,
        val email: String? = null
    )

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    private fun registerUser(textName: String, textEmail: String, textPassword: String) {
        if (isNetworkAvailable()) {
            auth.createUserWithEmailAndPassword(textEmail, textPassword).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(textName)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

                            val userID = auth.currentUser!!.uid
                            val documentReference = store.collection("users").document(
                                userID
                            )
                            val user = HashMap<String, Any>()
                            user["uid"] = userID
                            user["name"] = textName
                            user["email"] = textEmail
                            user["password"] = textPassword
                            documentReference.set(user).addOnSuccessListener {
                                Log.d(TAG, "onSuccess: user profile is created for $userID")
                            } .addOnFailureListener {
                                Log.d(TAG, "onFailure: $it")
                            }

                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to update profile: ${profileTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorMessage = when (val exception = task.exception) {
                        is FirebaseAuthWeakPasswordException -> "Weak password. Please choose a stronger password."
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                        is FirebaseAuthUserCollisionException -> "This email is already registered."
                        is FirebaseNetworkException -> "Network error. Please check your internet connection."
                        is FirebaseAuthException -> {
                            when (exception.errorCode) {
                                "ERROR_NETWORK_REQUEST_FAILED" -> "A network error (such as timeout, interrupted connection or unreachable host) has occurred."
                                else -> exception.message ?: "Registration failed"
                            }
                        }
                        else -> exception?.message ?: "Registration failed"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No network connection. Please check your internet connection.", Toast.LENGTH_SHORT).show()
        }
    }


}