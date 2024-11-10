package com.example.splashscreen

import android.media.Image
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
import org.w3c.dom.Text
import retrofit2.Response

class PaymentActivity : AppCompatActivity() {

    private val shippingFee: Long = 300000

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
        setContentView(R.layout.payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            val userId = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)?.toLong()
            if (userId != null) {
                fetchCartItems(userId.toLong())
            }
        }

        findViewById<ImageButton>(R.id.backbutton).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.shippingFee).text = "Rp. ${formatWithDots(shippingFee.toString())}"
    }

    // Fetch user ID by email to retrieve the avatar's path
    private suspend fun getUserIdByEmail(email: String): Int? {
        val requestBody = mapOf("user_email" to email)
        val response: Response<Int> = RetrofitClient.instance.getUserIdByEmail(requestBody)

        return if (response.isSuccessful) {
            Log.d("APIResponse", "User ID retrieved: ${response.body()}")
            response.body() // Return the user ID directly
        } else {
            Log.e("APIError", "Failed to retrieve user ID: ${response.errorBody()?.string()}")
            null
        }
    }

    // Merge cart items by grouping them based on product ID and summing quantities and prices
    private fun mergeCartItems(cartItems: List<Map<String, Any>>): List<Map<String, Any>> {
        val mergedItems = mutableListOf<Map<String, Any>>()

        // Group items by product ID
        val groupedItems = cartItems.groupBy { it["product_id"] }

        for ((productId, items) in groupedItems) {
            var totalBarang = 0
            var totalHarga = 0L
            var productName: String? = null
            var productImage: String? = null
            var productKategori: String? = null

            // Sum the quantities and prices for each group
            items.forEach { item ->
                totalBarang += (item["total_barang"] as? String)?.toIntOrNull() ?: 0
                totalHarga += (item["total_harga"] as? String)?.toLongOrNull() ?: 0L

                // Extract product details
                productName = item["product_name"] as? String
                productImage = item["product_image"] as? String
                productKategori = item["kategori_name"] as? String
            }

            // Create a merged item with the accumulated data
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

    // Fetch cart items from the API and process them
    private suspend fun fetchCartItems(userId: Long) {
        try {
            val requestBody = mapOf("user_id_param" to userId)
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

    // Update the calculateTotalPrice function to include the shipping fee
    private fun calculateTotalPrice(cartItems: List<Map<String, Any>>): Long {
        var subTotal: Long = 0
        for (cartItem in cartItems) {
            subTotal += (cartItem["total_harga"] as? String)?.toLongOrNull() ?: 0L
        }
        findViewById<TextView>(R.id.subTotal).text = "Rp. ${formatWithDots(subTotal.toString())}"
        return subTotal + shippingFee
    }

    // Update the displayCartItems function to show the total price with shipping fee
    private fun displayCartItems(cartItems: List<Map<String, Any>>, initialTotalPrice: Long) {
        val cartContainer = findViewById<LinearLayout>(R.id.cart_container)
        cartContainer.removeAllViews()
        var subTotal = initialTotalPrice

        for (cartItem in cartItems) {
            val cartItemView = layoutInflater.inflate(R.layout.payment_products, cartContainer, false)

            // Extract product details
            val productName = cartItem["product_name"] as? String ?: "Unknown Product"
            val productPrice = cartItem["total_harga"] as? String ?: "0"
            val productImage = cartItem["product_image"] as? String ?: ""
            val productKategori = cartItem["kategori_name"] as? String ?: "Unknown Category"
            val totalBarang = cartItem["total_barang"] as? String ?: "1"
            val productId = (cartItem["product_id"] as? Number)?.toLong() ?: 0L

            // Parse price and quantity
            var itemQuantity = totalBarang.toIntOrNull() ?: 1
            val itemPrice = productPrice.toLongOrNull() ?: 0L

            // Set UI data for product name, category, and price
            cartItemView.findViewById<TextView>(R.id.product_name).text = productName
            val quantityTextView = cartItemView.findViewById<TextView>(R.id.product_quantity)
            quantityTextView.text = "Quantity: $itemQuantity"



            // Display the initial calculated price for the item
            val produkPriceTextView = cartItemView.findViewById<TextView>(R.id.product_price)
            produkPriceTextView.text = "Rp. ${formatWithDots((itemPrice).toString())}"

            // Load product image if available
            val productImageView = cartItemView.findViewById<ImageView>(R.id.product_image)
            if (productImage.isNotEmpty()) {
                loadProductImage(productImage, productImageView)
            }

            // Add the view to the container
            cartContainer.addView(cartItemView)
        }

        // Update the total price (SubTotal + Shipping Fee) at the bottom
        val totalPrice = subTotal
        updateTotalPrice(totalPrice)
    }

    // Update the total price displayed at the bottom of the cart
    private fun updateTotalPrice(totalPrice: Long) {
        val totalPriceTextView = findViewById<TextView>(R.id.totalPrice)
        totalPriceTextView.text = "Rp. ${formatWithDots(totalPrice.toString())}"
        findViewById<TextView>(R.id.total).text = "Rp. ${formatWithDots(totalPrice.toString())}"
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

    // Format the price with dots (e.g., 1000 => 1.000)
    private fun formatWithDots(price: String): String {
        return price.reversed().chunked(3).joinToString(".").reversed()
    }

}