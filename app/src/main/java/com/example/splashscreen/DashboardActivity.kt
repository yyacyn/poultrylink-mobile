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
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response

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
            val userEmail = auth.retrieveUserForCurrentSession(updateSession = true).email

            // Check if the user is authenticated and retrieve their display name
            if (userEmail != null) {
                val requestBody = mapOf("p_email" to userEmail)
                val response: Response<String> = RetrofitClient.instance.getUserByEmail(requestBody)

                // Check if the response is successful
                if (response.isSuccessful) {
                    // Extract the display name from the response body
                    val displayName = response.body()?.removeSurrounding("\"") // Remove quotes if they are included

                    // Update the TextView with the display name
                    greetUser.text = "Welcome, ${displayName ?: "User"}" // Fallback if displayName is null
                    Log.d("MyTag", "Display Name: $displayName")
                } else {
                    Log.e("MyTag", "Error retrieving display name: ${response.message()}")
                    greetUser.text = "Welcome, User" // Fallback if user retrieval fails
                }
            } else {
                Log.d("MyTag", "User not found")
                greetUser.text = "Welcome, Guest" // Fallback if user is not found
            }
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
}