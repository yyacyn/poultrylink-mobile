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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
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

    private var totalPrice: Long = 0
    private var cartItems: List<Map<String, Any>> = emptyList()



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
        val checkOut = findViewById<MaterialButton>(R.id.buttonPurchase)

        mycart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        checkOut.setOnClickListener {
            lifecycleScope.launch {
                updateCartItemsForCheckout() // Update items before proceeding
                val intent = Intent(this@CartActivity, PaymentActivity::class.java)
                startActivity(intent)
            }
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
        this.cartItems = cartItems  // Store the cart items for later use
        val cartContainer = findViewById<LinearLayout>(R.id.cart_container)
        cartContainer.removeAllViews()
        var currentTotalPrice = initialTotalPrice

        Log.d("CartDisplay", "Displaying ${cartItems.size} items")

        for (cartItem in cartItems) {
            val cartItemView = layoutInflater.inflate(R.layout.cartcart, cartContainer, false)

            // Extract data with better null handling
            val productId = (cartItem["product_id"] as? Number)?.toLong() ?: run {
                Log.e("CartError", "Invalid product_id for item: $cartItem")
            }

            val productName = cartItem["product_name"] as? String ?: "Unknown Product"
            val productPrice = (cartItem["total_harga"] as? String)?.toLongOrNull() ?: 0L
            val totalBarang = (cartItem["total_barang"] as? String)?.toIntOrNull() ?: 1
            val productKategori = cartItem["kategori_name"] as? String ?: "Unknown Category"
            val productImage = cartItem["product_image"] as? String ?: ""

            Log.d("CartItem", "Processing item: $productId, Quantity: $totalBarang, Price: $productPrice")

            // Initialize views
            val quantityTextView = cartItemView.findViewById<TextView>(R.id.quantity)
            val produkPriceTextView = cartItemView.findViewById<TextView>(R.id.produkPrice)


            cartItemView.findViewById<TextView>(R.id.produkName).text = productName
            cartItemView.findViewById<TextView>(R.id.kategoriProduk).text = productKategori

            var currentQuantity = totalBarang
            val singleItemPrice = if (currentQuantity > 0) productPrice / currentQuantity else 0L

            quantityTextView.text = currentQuantity.toString()

            // Load product image if available
            val productImageView = cartItemView.findViewById<ImageView>(R.id.produkImage)
            if (productImage.isNotEmpty()) {
                loadProductImage(productImage, productImageView)
            }

            // Update UI
            quantityTextView.text = currentQuantity.toString()
            updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)

            // Handle quantity changes
            cartItemView.findViewById<ImageButton>(R.id.plus).setOnClickListener {
                currentQuantity++
                updateCartItemQuantity(productId.toLong(), currentQuantity)
                quantityTextView.text = currentQuantity.toString()
                val newItemTotal = singleItemPrice * currentQuantity
                updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)
                currentTotalPrice += singleItemPrice
                updateTotalPrice(currentTotalPrice)

                Log.d("CartUpdate", "Increased quantity for product $productId to $currentQuantity")
            }

            cartItemView.findViewById<ImageButton>(R.id.minus).setOnClickListener {
                if (currentQuantity > 1) {
                    currentQuantity--
                    updateCartItemQuantity(productId.toLong(), currentQuantity)
                    quantityTextView.text = currentQuantity.toString()
                    val newItemTotal = singleItemPrice * currentQuantity
                    updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)
                    currentTotalPrice -= singleItemPrice
                    updateTotalPrice(currentTotalPrice)

                    Log.d("CartUpdate", "Decreased quantity for product $productId to $currentQuantity")
                }
            }

            // Add the view to the container
            cartContainer.addView(cartItemView)
        }

        updateTotalPrice(currentTotalPrice)
    }

    private fun updateCartItemQuantity(productId: Long, quantity: Int) {
        lifecycleScope.launch {
            try {
                val request = CartUpdateRequest(
                    p_cart_id = productId,
                    p_quantity = quantity
                )

                Log.d("CartUpdate", "Sending update request: $request")

                val response = RetrofitClient.instance.updateCartItem(request)
                if (response.isSuccessful) {
                    Log.d("CartUpdate", "Successfully updated cart item $productId to quantity $quantity")
                } else {
                    Log.e("CartUpdate", "Failed to update cart: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("CartUpdate", "Exception updating cart: ${e.message}", e)
            }
        }
    }

    private suspend fun updateCartItemsForCheckout() {
        try {
            Log.d("Checkout", "Starting checkout process with ${cartItems.size} items")

            cartItems.forEach { cartItem ->
                val productId = (cartItem["product_id"] as? Number)?.toLong() ?: run {
                    Log.e("Checkout", "Invalid product_id in cart item: $cartItem")
                    return@forEach
                }

                val quantity = (cartItem["total_barang"] as? String)?.toIntOrNull() ?: run {
                    Log.e("Checkout", "Invalid quantity in cart item: $cartItem")
                    return@forEach
                }

                val request = CartUpdateRequest(
                    p_cart_id = productId,
                    p_quantity = quantity
                )

                Log.d("Checkout", "Updating item: $productId with quantity: $quantity")

                val response = RetrofitClient.instance.updateCartItem(request)
                if (response.isSuccessful) {
                    Log.d("Checkout", "Successfully updated item $productId")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("Checkout", "Failed to update item $productId: $errorBody")
                }
            }
        } catch (e: Exception) {
            Log.e("Checkout", "Exception during checkout: ${e.message}", e)
        }
    }


    // Helper function to update the price of each item based on quantity
    private fun updateItemPrice(produkPriceTextView: TextView, itemPrice: Long, quantity: Int) {
        val totalItemPrice = itemPrice * quantity // Calculate total price based on quantity
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
            .placeholder(R.drawable.sekar)
            .error(R.drawable.emiya)
            .into(imageView)
    }
}