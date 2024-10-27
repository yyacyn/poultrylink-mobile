package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

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
        setContentView(R.layout.dashboard)

        val greetUser = findViewById<TextView>(R.id.greet)
        val userPfp = findViewById<ImageView>(R.id.user_pfp)

        // Update user greeting and load avatar
        lifecycleScope.launch {
            updateUserGreeting(greetUser)
            uploadAndDisplayAvatar("avatar.jpg", R.drawable.emiya)
        }
    }

    private suspend fun updateUserGreeting(greetUser: TextView) {
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

    private fun getDrawableAsByteArray(drawableId: Int): ByteArray {
        val drawable = resources.getDrawable(drawableId, null) as BitmapDrawable
        val bitmap = drawable.bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private suspend fun updateUserAvatarPath(email: String, filePath: String) {
        supabase.postgrest["users"].update(mapOf("avatar_path" to filePath)) {
            filter { eq("email", email) }
        }
    }

    private fun uploadAndDisplayAvatar(filename: String, drawableId: Int) {
        lifecycleScope.launch {
            val currentEmail = supabase.auth.retrieveUserForCurrentSession().email ?: return@launch
            val avatarPath = "avatars/$currentEmail/$filename"

            try {
                // Switch to background thread for network operations
                withContext(Dispatchers.IO) {
                    // Check if the user already has an avatar
                    val existingFiles = supabase.storage.from("avatar").list("avatars/$currentEmail")
                    val imageData = getDrawableAsByteArray(drawableId)

                    if (existingFiles.isNotEmpty()) {
                        // Update the existing file
                        supabase.storage.from("avatar").update(avatarPath, imageData)
                        Log.d("Supabase", "Existing avatar updated: $avatarPath")
                    } else {
                        // Upload a new avatar
                        supabase.storage.from("avatar").upload(avatarPath, imageData)
                        Log.d("Supabase", "New avatar uploaded: $avatarPath")
                    }

                    // Update avatar path in user's database record
                    updateUserAvatarPath(currentEmail, avatarPath)
                }

                // Load the image from Supabase Storage URL into ImageView with Glide
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$avatarPath"
                Glide.with(this@DashboardActivity)
                    .load(imageUrl)
                    .skipMemoryCache(true) // skip memory cache
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // skip disk cache
                    .into(findViewById(R.id.user_pfp))
                Log.d("Supabase", "Avatar displayed successfully")

            } catch (e: Exception) {
                Log.e("SupabaseUploadError", "Failed to upload or display avatar: ${e.message}")
            }
        }
    }

}
