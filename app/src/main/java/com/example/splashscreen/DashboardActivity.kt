package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ" // Replace with your actual Supabase key
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard)

        val buttonProduk = findViewById<CardView>(R.id.produk1)
        val buttoncart = findViewById<ImageButton>(R.id.cart)
        val greetUser = findViewById<TextView>(R.id.greet)
        val userPfp = findViewById<ImageView>(R.id.user_pfp)

        lifecycleScope.launch {
            // Retrieve user info
            val auth = supabase.auth
            val userEmail = auth.retrieveUserForCurrentSession(updateSession = true).email

            // Check if the user is authenticated
            if (userEmail != null) {
                val requestBody = mapOf("p_email" to userEmail)
                val response: Response<String> = RetrofitClient.instance.getUserByEmail(requestBody)

                // Check if the response is successful
                if (response.isSuccessful) {
                    val displayName = response.body()?.removeSurrounding("\"")
                    greetUser.text = "Welcome, ${displayName ?: "User"}"
                } else {
                    greetUser.text = "Welcome, User"
                }
            } else {
                greetUser.text = "Welcome, Guest"
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

        // Load image from Supabase Storage
        loadImageFromSupabase("sekar.jpg") // Specify your file path here
    }

    private fun loadImageFromSupabase(filePath: String) {
        lifecycleScope.launch {
            try {
                // Construct the public URL to the object in the storage bucket
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath"

                //local image url
                val localImageUrl = "/drawable/$filePath"
                // Use Glide to load the image into the ImageView
                Glide.with(this@DashboardActivity)
                    .load(imageUrl)
                    .into(findViewById<ImageView>(R.id.user_pfp)) // Load into your ImageView

                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }
}
