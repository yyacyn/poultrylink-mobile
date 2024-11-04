package com.example.splashscreen

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.util.Log
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
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.launch
import retrofit2.Response

class ProfilActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val greetUser = findViewById<TextView>(R.id.username)
        val userPfp = findViewById<ImageView>(R.id.profile_image)

//        Navigation()

        // Update user greeting and load avatar
        lifecycleScope.launch {
            updateUserGreeting(greetUser)

            // Get user ID from email and load avatar
            val userEmail = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
            if (userEmail != null) {
                loadImageFromSupabase("$userEmail/1.jpg")
            }
        }
    }

//    private fun Navigation() {
//        val buttoncart = findViewById<ImageButton>(R.id.cart)
//        val buttonProduk = findViewById<CardView>(R.id.produkcard)
//        val buttonHome = findViewById<ImageButton>(R.id.btnhome)
//        val buttonMarket = findViewById<ImageButton>(R.id.btnmarket)
//        val buttonHistory = findViewById<ImageButton>(R.id.btnhistory)
//        val buttonProfile = findViewById<ImageButton>(R.id.btnprofil)
//
//        buttoncart.setOnClickListener {
//            startActivity(Intent(this, CartActivity::class.java))
//        }
//
//        buttonProduk.setOnClickListener {
//            startActivity(Intent(this, ProdukActivity::class.java))
//        }
//
//        buttonHistory.setOnClickListener {
////            startActivity(this,H)
//        }
//
//        buttonProfile.setOnClickListener {
//            startActivity(Intent(this, ProfilActivity::class.java))
//        }
//
//        buttonHome.setOnClickListener {
//            startActivity(Intent(this, DashboardActivity::class.java))
//        }
//
////        buttonMarket.setOnClickListener {
////            startActivity(Intent(this, MarketActivity::class.java))
////        }
//    }

    private suspend fun updateUserGreeting(greetUser: TextView) {
        val auth = supabase.auth
        val userEmail = auth.retrieveUserForCurrentSession(updateSession = true).email

        if (userEmail != null) {
            val requestBody = mapOf("p_email" to userEmail)
            val response: Response<String> = RetrofitClient.instance.getUserByEmail(requestBody)

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

    private suspend fun getUserIdByEmail(email: String): Int? {
        val requestBody = mapOf("user_email" to email)
        val response: Response<Int> = RetrofitClient.instance.getUserIdByEmail(requestBody)

        if (response.isSuccessful) {
            Log.d("APIResponse", "User ID retrieved: ${response.body()}")
            return response.body() // Return the user ID directly
        } else {
            Log.e("APIError", "Failed to retrieve user ID: ${response.errorBody()?.string()}")
            return null
        }
    }

    private fun loadImageFromSupabase(filePath: String) {
        lifecycleScope.launch {
            try {
                // Construct the public URL to the object in the storage bucket
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath"

                // Use Glide to load the image into the ImageView
                Glide.with(this@ProfilActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(findViewById<CircleImageView>(R.id.profile_image))
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }
}