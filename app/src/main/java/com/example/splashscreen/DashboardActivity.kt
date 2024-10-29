package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        val buttonProduk = findViewById<CardView>(R.id.produkcard)
        val buttoncart = findViewById<ImageButton>(R.id.cart)

        // Load products into the grid
        loadProducts()

        // Update user greeting and load avatar
        lifecycleScope.launch {
            updateUserGreeting(greetUser)
//            uploadAndDisplayAvatar("avatar.jpg", R.drawable.emiya)
            val currentEmail = supabase.auth.retrieveUserForCurrentSession().email ?: return@launch
            loadImageFromSupabase("/avatars/$currentEmail/avatar.jpg")
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

    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                val response = supabase.postgrest["produk"].select()
                if (response.data is String) {
                    Log.d("ProductLoad", "productformat: ${response.data}")
                    // If the response data is a JSON string
                    val productsJson = response.data
                    val gson = Gson()
                    val productType = object : TypeToken<Array<Products>>() {}.type

                    // Convert JSON string to an Array of Products
                    val products: Array<Products> = gson.fromJson(productsJson, productType)
                    Log.d("ProductLoad", "products: ${products.toList()}")

                    // Convert Array to a mutable list if needed
                    displayProducts(products.toList())
                } else {
                    Log.e("ProductLoadError", "Unexpected response format: ${response.data}")
                }
            } catch (e: Exception) {
                Log.e("ProductLoadError", "Failed to load products: ${e.message}")
            }
        }
    }

    private fun displayProducts(products: List<Products>) {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout) // Use the ID defined in dashboard.xml
        gridLayout.removeAllViews() // Clear any existing views

        for (product in products) {
            // Inflate the existing CardView layout from XML
            val cardView = layoutInflater.inflate(R.layout.product_card, gridLayout, false) // Assume your CardView XML layout is named card_product.xml

            // Bind the product data to the views
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)
            val productLocation = cardView.findViewById<TextView>(R.id.productLocation)

            // Load product image with Glide
            val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/${product.image}"
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.emiya)
                .error(R.drawable.sekar)
                .into(productImage)

            // Set product name, rating, and price
            productName.text = product.nama_produk
            productRating.text = product.rating.toString()
            productAmountRating.text = "(${product.reviews} Reviews)"
            productPrice.text = "Rp ${product.harga}"

            // Add the inflated CardView to the GridLayout
            gridLayout.addView(cardView)
        }
    }
}
