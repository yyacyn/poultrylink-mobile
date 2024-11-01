package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import java.text.NumberFormat
import java.util.Locale

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
        val buttoncart = findViewById<ImageButton>(R.id.cart)
        val buttonProduk = findViewById<CardView>(R.id.produkcard)

        // Load products into the grid
        loadProducts()

        // Update user greeting and load avatar
        lifecycleScope.launch {
            updateUserGreeting(greetUser)

            // Get user ID from email and load avatar
            val userId = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
            if (userId != null) {
                loadImageFromSupabase("/avatars/$userId/avatar.jpg")
            }
        }

        buttoncart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        buttonProduk.setOnClickListener {
            startActivity(Intent(this, ProdukActivity::class.java))
        }
    }

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
        val requestBody = mapOf("p_email" to email)
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
                Glide.with(this@DashboardActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(findViewById<ImageView>(R.id.user_pfp))
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

    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount) // This will automatically add dots without "Rp" symbol
    }

    private fun displayProducts(products: List<Products>) {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        gridLayout.removeAllViews() // Clear existing views

        for ((index, product) in products.withIndex()) {
            val cardView = layoutInflater.inflate(R.layout.product_card, gridLayout, false)
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)
            val productLocation = cardView.findViewById<TextView>(R.id.productLocation)

            // Load the first image for the product
            val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/${product.image}/1.jpg"
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.emiya)
                .error(R.drawable.sekar)
                .into(productImage)

            productName.text = product.nama_produk
            productRating.text = product.rating.toString()
            productAmountRating.text = "(${product.reviews} Reviews)"

            // Use formatWithDots to format the price
            val value = formatWithDots(product.harga)
            productPrice.text = "Rp. $value"

            val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
            if (index % 2 == 0) {
                params.setMargins(30, 20, 10, 20)
            } else {
                params.setMargins(20, 20, 10, 20)
            }

            cardView.setOnClickListener {
                val intent = Intent(this, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.image) // Pass the image base name
                    putExtra("productRating", product.rating)
                    putExtra("productPrice", product.harga)
                    putExtra("productDesc", product.deskripsi)
                    putExtra("supplierId", product.supplier_id)
                }
                startActivity(intent)
            }

            cardView.layoutParams = params
            gridLayout.addView(cardView)
        }
    }

    private suspend fun loadFirstImageFromSupabase(folderPath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // List all files in the specified folder
                val files = supabase.storage.from("products").list(folderPath)

                // Check if files are retrieved and get the first file
                if (files.isNotEmpty()) {
                    val firstFile = files[0].name // Get the name of the first file
                    // Construct the public URL to the first file
                    return@withContext "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/$folderPath/$firstFile"
                } else {
                    Log.e("ImageLoadError", "No images found in the folder: $folderPath")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load images: ${e.message}")
                return@withContext null
            }
        }
    }
}
