package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

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
        setContentView(R.layout.cart)
        val mycart = findViewById<TextView>(R.id.mycart)
        val onprocess = findViewById<TextView>(R.id.onprocess)
        val complete = findViewById<TextView>(R.id.complete)

        findViewById<ImageButton>(R.id.backbutton).setOnClickListener {
            finish()
        }
        lifecycleScope.launch {
            val userId = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)?.toLong()
            if (userId != null) {
                fetchCartItems(userId.toLong())
            }
        }


        val produkName = findViewById<TextView>(R.id.produkName)
        val produkPrice = findViewById<TextView>(R.id.produkPrice)
        val produkImage = findViewById<ImageView>(R.id.produkImage)
        val produkKategori = findViewById<TextView>(R.id.kategoriProduk)

        mycart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        onprocess.setOnClickListener {
            val intent = Intent(this, CartOnprogressActivity::class.java)
            startActivity(intent)
        }

        complete.setOnClickListener {
            val intent = Intent(this, CartCompleteActivity::class.java)
            startActivity(intent)
        }
    }
    // get user's id by email to get their avatar's path
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


    private fun mergeCartItems(cartItems: List<Map<String, Any>>): List<Map<String, Any>> {
        val mergedItems = mutableListOf<Map<String, Any>>()

        // Group cart items by product_id
        val groupedItems = cartItems.groupBy { it["product_id"] }

        for ((productId, items) in groupedItems) {
            var totalBarang = 0
            var totalHarga = 0L
            var productName: String? = null
            var productImage: String? = null
            var productKategori: String? = null

            // Iterate through each group and sum the quantities and prices
            for (item in items) {
                totalBarang += (item["total_barang"] as? String)?.toIntOrNull() ?: 0
                totalHarga += (item["total_harga"] as? String)?.toLongOrNull() ?: 0L

                // Get product details (assume same for all items in the group)
                productName = item["product_name"] as? String
                productImage = item["product_image"] as? String
                productKategori = item["kategori_name"] as? String
            }

            // Create a merged item for this product
            val mergedItem = mutableMapOf<String, Any>().apply {
                put("product_id", productId ?: 0L)
                put("product_name", productName ?: "Unknown Product")
                put("total_barang", totalBarang.toString())
                put("total_harga", totalHarga.toString())
                put("product_image", productImage ?: "")
                put("kategori_name", productKategori ?: "Unknown Category")
            }

            // Add the merged item to the result list
            mergedItems.add(mergedItem)
        }

        return mergedItems
    }

    private suspend fun fetchCartItems(userId: Long) {
        try {
            // Wrap the userId in a Map as expected by the API
            val requestBody = mapOf("user_id_param" to userId)

            // Make the API call to get cart items
            val response = RetrofitClient.instance.getCartItems(requestBody)

            if (response.isSuccessful && response.body() != null) {
                val cartItems = response.body()!!

                // Merge items with the same product_id
                val mergedCartItems = mergeCartItems(cartItems)

                // Calculate the total price
                val totalPrice = calculateTotalPrice(mergedCartItems)

                // Display the merged cart items
                displayCartItems(mergedCartItems, totalPrice)
            } else {
                Log.e("CartFetchError", "Error fetching cart items: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("CartFetchError", "Exception: ${e.message}")
        }
    }

    // Calculate the total price by summing the total_harga of all cart items
    private fun calculateTotalPrice(cartItems: List<Map<String, Any>>): Long {
        var totalPrice: Long = 0
        for (cartItem in cartItems) {
            totalPrice += (cartItem["total_harga"] as? String)?.toLongOrNull() ?: 0L
        }
        return totalPrice
    }

    private fun displayCartItems(cartItems: List<Map<String, Any>>, initialTotalPrice: Long) {
        val cartContainer = findViewById<LinearLayout>(R.id.cart_container)
        cartContainer.removeAllViews()
        var totalPrice = initialTotalPrice

        for (cartItem in cartItems) {
            val cartItemView = layoutInflater.inflate(R.layout.cartcart, cartContainer, false)

            // Extract data from the map
            val productName = cartItem["product_name"] as? String ?: "Unknown Product"
            val productPrice = cartItem["total_harga"] as? String ?: "0"
            val productImage = cartItem["product_image"] as? String ?: ""
            val productKategori = cartItem["kategori_name"] as? String ?: "Unknown Category"
            val totalBarang = cartItem["total_barang"] as? String ?: "1" // Default to 1 if not provided
            val productId = (cartItem["product_id"] as? Number)?.toLong() ?: 0L

            // Parse price and quantity
            var itemQuantity = totalBarang.toIntOrNull() ?: 1
            val itemPrice = productPrice.toLongOrNull() ?: 0L

            // Set initial UI data
            cartItemView.findViewById<TextView>(R.id.produkName).text = productName
            cartItemView.findViewById<TextView>(R.id.kategoriProduk).text = productKategori
            val produkPriceTextView = cartItemView.findViewById<TextView>(R.id.produkPrice)
            val quantityTextView = cartItemView.findViewById<TextView>(R.id.quantity)
            quantityTextView.text = itemQuantity.toString()

            // Display the initial calculated price for the item
            produkPriceTextView.text = "Rp. ${formatWithDots((itemPrice * itemQuantity).toString())}"

            // Load product image if available
            val productImageView = cartItemView.findViewById<ImageView>(R.id.produkImage)
            if (productImage.isNotEmpty()) {
                loadProductImage(productImage, productImageView)
            }

            // Plus and minus buttons for updating quantity and price
            val plusButton = cartItemView.findViewById<ImageButton>(R.id.plus)
            val minusButton = cartItemView.findViewById<ImageButton>(R.id.minus)

            plusButton.setOnClickListener {
                itemQuantity++
                quantityTextView.text = itemQuantity.toString()
                updateItemPrice(produkPriceTextView, itemPrice, itemQuantity)
                totalPrice += itemPrice
                updateTotalPrice(totalPrice)
            }

            minusButton.setOnClickListener {
                if (itemQuantity > 1) {
                    itemQuantity--
                    quantityTextView.text = itemQuantity.toString()
                    updateItemPrice(produkPriceTextView, itemPrice, itemQuantity)
                    totalPrice -= itemPrice
                    updateTotalPrice(totalPrice)
                }
            }

            // Add the view to the container
            cartContainer.addView(cartItemView)
        }

        // Set the initial total price
        updateTotalPrice(totalPrice)
    }

    // Helper function to update the price of each item based on quantity
    private fun updateItemPrice(produkPriceTextView: TextView, itemPrice: Long, quantity: Int) {
        val totalItemPrice = itemPrice * quantity
        produkPriceTextView.text = "Rp. ${formatWithDots(totalItemPrice.toString())}"
    }

    // Helper function to update the total price in the TextView
    private fun updateTotalPrice(totalPrice: Long) {
        val totalPriceTextView = findViewById<TextView>(R.id.totalPrice)
        val formattedTotalPrice = formatWithDots(totalPrice.toString())
        totalPriceTextView.text = "Rp. $formattedTotalPrice"
    }


    // Price formatting with dots (e.g., 1,000,000 -> 1.000.000)
    private fun formatWithDots(price: String): String {
        return try {
            val amount = price.toLong()
            val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
            format.format(amount) // This will automatically add dots without the "Rp" symbol
        } catch (e: Exception) {
            // If the price is invalid or cannot be parsed, return the original price as is
            price
        }
    }

    // Load product image from the URL
    private fun loadProductImage(filePath: String, imageView: ImageView) {
        val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$filePath/1.jpg"

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.sekar) // Replace with your placeholder
            .error(R.drawable.emiya) // Replace with your error image
            .into(imageView)
    }

}