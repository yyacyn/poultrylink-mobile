package com.example.splashscreen

import android.content.Intent
import android.content.res.ColorStateList
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private var totalPrice: Long = 0
    private var cartItems: List<CartData> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cart)
        val mycart = findViewById<TextView>(R.id.mycart)
        val onprocess = findViewById<TextView>(R.id.onprocess)
        val complete = findViewById<TextView>(R.id.complete)

        val token = "Bearer ${getStoredToken()}"

        findViewById<ImageButton>(R.id.backbutton).setOnClickListener {
            onBackPressed()
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


        onprocess.setOnClickListener {
            val intent = Intent(this, CartOnprogressActivity::class.java)
            startActivity(intent)
        }

        complete.setOnClickListener {
            val intent = Intent(this, CartCompleteActivity::class.java)
            startActivity(intent)
        }

        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                fetchCartItems(token, userId)
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }


    private fun mergeCartItems(cartItems: List<CartData>): List<CartData> {
        val mergedItemsMap = mutableMapOf<String, CartData>()

        for (cartItem in cartItems) {
            val key = "${cartItem.user_id}-${cartItem.produk_id}"

            if (mergedItemsMap.containsKey(key)) {
                val existingItem = mergedItemsMap[key]!!
                val mergedTotalBarang = existingItem.total_barang.toInt() + cartItem.total_barang.toInt()
                val mergedTotalHarga = existingItem.total_harga.toLong() + cartItem.total_harga.toLong()

                // Update the existing item with new totals
                mergedItemsMap[key] = existingItem.copy(
                    total_barang = mergedTotalBarang.toString(),
                    total_harga = mergedTotalHarga.toString()
                )
            } else {
                // Add the new item to the map
                mergedItemsMap[key] = cartItem
            }
        }

        return mergedItemsMap.values.toList()
    }

    private fun getUser(token: String, callback: (Long?) -> Unit) {
        RetrofitClient.instance.getUser(token)
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        val userId = response.body()?.id
                        Log.d("getUser", "User ID: $userId")
                        if (userId != null) {
                            callback(userId.toLong())
                        } // Return the user ID via callback
                    } else {
                        Log.e("FetchCarts", "Error: ${response.code()}")
                        callback(null) // Return null if there's an error
                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {
                    Log.e("FetchCarts", "Network Error: ${t.message}")
                    callback(null) // Return null if there's a failure
                }
            })
    }

    private fun fetchCartItems(token: String, userId: Long) {
        RetrofitClient.instance.getAllCarts(token)
            .enqueue(object : Callback<CartResponse> {
                override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                    if (response.isSuccessful) {
                        val carts = response.body()?.data ?: emptyList()
                        Log.d("carts", "$carts")
                        val filteredCarts = carts.filter { it.deleted_at == null && it.user_id == userId.toString() }

                        Log.d("filteredCarts", "$filteredCarts")

                        // Merge cart items with the same user_id and produk_id
                        val mergedCarts = mergeCartItems(filteredCarts)

                        // Display the merged cart items
                        val totalPrice = calculateTotalPrice(mergedCarts)
                        displayCartItems(mergedCarts, totalPrice)

                        Log.d("MergedCarts", "$mergedCarts")
                    } else {
                        Log.e("FetchCarts", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    Log.e("FetchCarts", "Network Error: ${t.message}")
                }
            })
    }


    // Calculate the total price by summing the total_harga of all cart items
    private fun calculateTotalPrice(cartItems: List<CartData>): Long {
        var totalPrice: Long = 0
        for (cartItem in cartItems) {
            totalPrice += (cartItem.total_harga as? String)?.toLongOrNull() ?: 0L
        }
        return totalPrice
    }

    override fun onResume() {
        super.onResume()

        // Retrieve the token and update profile
        val token = "Bearer ${getStoredToken().toString()}"
        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                fetchCartItems(token, userId)
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }
    }


    private fun displayCartItems(cartItems: List<CartData>, initialTotalPrice: Long) {
        this.cartItems = cartItems  // Store the cart items for later use
        val cartContainer = findViewById<LinearLayout>(R.id.cart_container)
        val checkOut = findViewById<MaterialButton>(R.id.buttonPurchase)
        val noCartTextView = TextView(this).apply {
            text = "Your cart is empty. Add items to get started!"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@CartActivity, R.color.gray))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        cartContainer.removeAllViews()

        if (cartItems.isEmpty()) {
            // Disable the checkout button and change its color when cart is empty
            checkOut.isEnabled = false // Disable button
            checkOut.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this@CartActivity, R.color.gray))) // Change to a gray color
        } else {
            // Enable the checkout button and set a different color when cart is not empty
            checkOut.isEnabled = true // Enable button
            checkOut.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this@CartActivity, R.color.orange))) // Change to active color
        }

        // Get the first cart ID

        Log.d("CartDisplay", "Displaying ${cartItems.size} items")

        // If the cart is empty
        if (cartItems.isEmpty()) {
            cartContainer.addView(noCartTextView)
            return
        }

        var currentTotalPrice = initialTotalPrice

        for (cartItem in cartItems) {
            val cartItemView = layoutInflater.inflate(R.layout.cartcart, cartContainer, false)

            val productName = cartItem.barang.nama_produk
            val productImage = cartItem.barang.image
            val productKategori = cartItem.barang.kategori_id
            val productId = cartItem.produk_id
            val totalBarang = cartItem.total_barang.toInt()
            val productPrice = cartItem.total_harga.toLong()
            val suppliersName = cartItem.barang.supplier?.nama_toko
            val userId = cartItem.user_id
            val cartId = cartItem.id

            Log.d("CartItem", "Id form the merged cart: $cartId")

            Log.d("CartItem", "Processing item: $productId, Quantity: $totalBarang, Price: $productPrice")

            // Initialize views
            val quantityTextView = cartItemView.findViewById<TextView>(R.id.quantity)
            val produkPriceTextView = cartItemView.findViewById<TextView>(R.id.produkPrice)

            cartItemView.findViewById<TextView>(R.id.produkName).text = productName
            if (productKategori == "1" || productKategori == "2" ){
                cartItemView.findViewById<TextView>(R.id.kategoriProduk).text = "Poultry"
            } else if (productKategori == "3" || productKategori == "4"){
                cartItemView.findViewById<TextView>(R.id.kategoriProduk).text = "Meat"
            } else if (productKategori == "5" || productKategori == "6"){
                cartItemView.findViewById<TextView>(R.id.kategoriProduk).text = "Egg"
            } else {
                cartItemView.findViewById<TextView>(R.id.kategoriProduk).text = "Seed"
            }
            cartItemView.findViewById<TextView>(R.id.suppliersName).text = suppliersName

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

            val token = "Bearer ${getStoredToken()}"
            val deleteCartButton = cartItemView.findViewById<ImageButton>(R.id.deleteCart)

            deleteCartButton.setOnClickListener {
                // Create an AlertDialog
                val alertDialog = AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this cart?")
                    .setCancelable(false) // Set to false so user must choose either Yes or No
                    .setPositiveButton("Yes") { dialog, which ->
                        // Proceed with finishing the activity if user confirms
                        deleteCart(token, productId.toInt(), userId.toInt())

                        // Remove the cart item view from the container
                        cartContainer.removeView(cartItemView)

                        // Update the total price and cart items
                        currentTotalPrice -= singleItemPrice * currentQuantity
                        updateTotalPrice(currentTotalPrice)

                        // Remove the item from the cartItems list
                        val updatedCartItems = this.cartItems.filterNot { it.produk_id == productId }
                        this.cartItems = updatedCartItems

                        // Show "cart is empty" message if the list is now empty
                        if (updatedCartItems.isEmpty()) {
                            cartContainer.addView(noCartTextView)
                        }
                    }
                    .setNegativeButton("No") { dialog, which ->
                        // Dismiss the dialog if user cancels
                        dialog.dismiss()
                    }

                // Show the alert dialog
                alertDialog.show()

            }

            // Handle quantity changes
            cartItemView.findViewById<ImageButton>(R.id.plus).setOnClickListener {
                currentQuantity++
                updateCartItemQuantity(productId.toLong(), currentQuantity)
                quantityTextView.text = currentQuantity.toString()
                val newItemTotal = singleItemPrice * currentQuantity
                updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)
                currentTotalPrice += singleItemPrice
                updateTotalPrice(currentTotalPrice)

                updateCartItemsForCheckout(token, cartId, currentQuantity)

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

                    updateCartItemsForCheckout(token, cartId, currentQuantity)

                    Log.d("CartUpdate", "Decreased quantity for product $productId to $currentQuantity")
                }
            }

            // Set up the checkout button
            checkOut.setOnClickListener {
                val intent = Intent(this@CartActivity, PaymentActivity::class.java)
                startActivity(intent)
            }

            // Add the view to the container
            cartContainer.addView(cartItemView)
        }
        updateTotalPrice(currentTotalPrice)

    }

    private fun deleteCart(token: String, productId: Int, userId: Int) {
        val request = DeleteCartRequest(productId, userId)
        RetrofitClient.instance.deleteCart(token, request)
            .enqueue(object : Callback<DeleteCartResponse> {
                override fun onResponse(
                    call: Call<DeleteCartResponse>,
                    response: Response<DeleteCartResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CartActivity, "Cart deleted successfully", Toast.LENGTH_SHORT).show()
                        Log.d("DeleteCart", "Item deleted successfully")
                        getUser(token) { userId ->
                            if (userId != null) {
                                Log.d("MainActivity", "Fetched User ID: $userId")
                                fetchCartItems(token, userId)
                            } else {
                                Log.e("MainActivity", "Failed to fetch User ID")
                            }
                        }
                    } else {
                        Log.e("DeleteCart", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<DeleteCartResponse>, t: Throwable) {
                    Log.e("DeleteCart", "Network Error: ${t.message}")
                }
            })
    }

    private fun updateCartItemQuantity(productId: Long, quantity: Int) {
    }

    private fun updateCartItemsForCheckout(token: String, cartId: Long, quantity: Int) {
        val updateCartRequest = UpdateCartRequest(
            cartId.toInt(),
            quantity
        )
        Log.d("CartUpdate", "Updating cart items for checkout: $updateCartRequest")
        RetrofitClient.instance.updateCart(token, updateCartRequest)
            .enqueue(object : Callback<CartResponse> {
                override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                    if (response.isSuccessful) {
                        val messageResponse = response.body()?.message
                        Log.d("CartUpdateSuccess", "$messageResponse")
                    } else {
                        Log.e("UpdateCartError", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<CartResponse>, t: Throwable) {
                    Log.e("CartError", "Network Error: ${t.message}")
                }
            })
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
    private fun loadProductImage(filePath: String, imageView: ImageView, forceRefresh: Boolean = false) {

        try {
            val baseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$filePath/1.jpg"
            val imageUrl = if (forceRefresh) {
                "$baseUrl?t=${System.currentTimeMillis()}"
            } else {
                baseUrl
            }

            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .transition(DrawableTransitionOptions.withCrossFade())
                .priority(Priority.HIGH)
                .error(R.drawable.sekar)
                .into(imageView)
        } catch (e: Exception) {
            Log.e("ImageLoadError", "Failed to load product image: ${e.message}")
        }
    }
}