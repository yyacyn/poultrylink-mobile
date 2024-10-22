package com.example.homepage

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splashscreen.PilihanLoginActivity
import com.example.splashscreen.R
import com.example.splashscreen.SignInActivity
//import com.example.splashscreen.SignInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var logout: Button
    lateinit var user_details: TextView
    private lateinit var store: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        auth = FirebaseAuth.getInstance()
        store = FirebaseFirestore.getInstance()
        userId = auth.currentUser!!.uid
        val documentReference = store.collection("users").document(userId)

        logout = findViewById(R.id.buttonLogout)
        user_details = findViewById(R.id.user_details)

        documentReference.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                // Handle the error
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val user_name = documentSnapshot.getString("name")
                user_details.setText("Welcome " + user_name)
            } else {
                Toast.makeText(this, "Document does not exist", Toast.LENGTH_SHORT).show()
            }
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, PilihanLoginActivity::class.java)
            startActivity(intent)
            finish()
        }
//        else {
//            val displayName = currentUser.displayName
//            user_details.setText("Welcome, $displayName")
//        }

        logout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PilihanLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}