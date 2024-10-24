package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.auth.User
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard)

        val buttonProduk = findViewById<CardView>(R.id.produk1)
        val buttoncart = findViewById<ImageButton>(R.id.cart)
        val greetUser = findViewById<TextView>(R.id.greet)

        lifecycleScope.launch {
            val auth = supabase.auth
            val userEmail =
                auth.retrieveUserForCurrentSession(updateSession = true).email.toString()
            Log.d("MyTag", userEmail)

            val greetUserWithName =
                supabase.postgrest["users"].select(columns = Columns.list("username")) {
                    filter {
                        eq("email", userEmail)
                    }
                }.toString()


            Log.d("MyTag", greetUserWithName)

            greetUser.text = "Welcome, ${greetUserWithName}"

        }


        buttonProduk.setOnClickListener {
            val intent = Intent(this, ProdukActivity::class.java)
            startActivity(intent)
        }

        buttoncart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }


    }

//    private suspend fun getUsernameByEmail(email: String): String? {
//        // Query the users table for the specified email
//        val greetUserWithName =
//            supabase.postgrest["users"].select(columns = Columns.list("username")) {
//                filter {
//                    eq("email", email)
//                }
//            }.toString()
//        // Log the result for debugging
//        Log.d("UserResult", greetUserWithName.toString())
//
//        // Check for errors in the result
//        if (greetUserWithName.error != null) {
//            Log.e("UserQueryError", "Error fetching user: ${greetUserWithName.error.message}")
//            return null // Return null if there was an error
//        }
//
//        // Check if any user was found and return the username
//        return if (greetUserWithName.data != null && greetUserWithName.data.isNotEmpty()) {
//            greetUserWithName.data[0]["username"] as? String
//        } else {
//            null // No user found
//        }
//    }
}