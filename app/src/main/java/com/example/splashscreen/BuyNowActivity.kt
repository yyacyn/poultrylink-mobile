package com.example.splashscreen

import ApiService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.yourapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BuyNowActivity : AppCompatActivity() {

    private var cartIds = mutableListOf<Long>()
    private val shippingFee: Long = 300000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buy_now)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val token = "Bearer ${getStoredToken()}"

        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productQty = intent.getIntExtra("productQty", 0)
        val productId = intent.getLongExtra("productId", 0)
        val cartId = intent.getLongExtra("cartId", 0)

        Log.d("cartIds", "$cartId")

        cartIds = mutableListOf(cartId)

        Log.d("cartIds", "$cartIds")

        Log.d("IntentData", "Product ID received: ${intent.getLongExtra("productId", 0)}")
        Log.d("IntentData", "Cart ID received: ${intent.getLongExtra("cartId", 0)}")

        val methodPayment = intent.getStringExtra("method")
        val imagePayment = intent.getStringExtra("image")

        findViewById<TextView>(R.id.shippingFee).text = "Rp. ${formatWithDots(shippingFee.toString())}"


        if (methodPayment != null && imagePayment != null) {
            findViewById<TextView>(R.id.paymentMethod).text = methodPayment.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            // Dynamically load the drawable resource by name
            val imageResourceId = resources.getIdentifier(imagePayment, "drawable", packageName)
            if (imageResourceId != 0) { // Check if the resource exists
                findViewById<ImageView>(R.id.paymentIcon).setImageResource(imageResourceId)
            } else {
                // Handle the case where the drawable resource is not found
                findViewById<ImageView>(R.id.paymentIcon).setImageResource(R.drawable.paymenticon)
            }
        }

        findViewById<ImageButton>(R.id.backbutton).setOnClickListener {
            finish()
        }
        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                getProfile(token)
                displayCheckoutItems(token)
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }

        findViewById<LinearLayout>(R.id.paymentContainer).setOnClickListener {
            val intent = Intent(this, MethodPaymentActivity::class.java).apply {
                putExtra("buyNow", true)
                putExtra("productName", productName)
                putExtra("productImage", productImage)
                putExtra("productPrice", productPrice)
                putExtra("productQty", productQty)
                putExtra("productId", productId)
                putExtra("cartId", cartId)
            }
            startActivity(intent)
        }

        setupBuyButton(token)

    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }


    private fun calculateTotalPrice(subTotal: Long): Long {

        // Format and display the subtotal in the TextView
        findViewById<TextView>(R.id.subTotal).text = "Rp. ${formatWithDots(subTotal.toString())}"

        // Add the shipping fee to the total
        val totalPrice = subTotal + shippingFee
        return totalPrice
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
    // Format the price with dots (e.g., 1000 => 1.000)
    private fun formatWithDots(price: String): String {
        return price.reversed().chunked(3).joinToString(".").reversed()
    }

    private fun getProfile(token: String?) {
        RetrofitClient.instance.getProfile(token ?: "")
            .enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {
                        val buyerData = response.body()?.data
                        val userkota = buyerData?.kota
                        val usernegara = buyerData?.negara
                        val useralamat = buyerData?.alamat
                        val userprovinsi = buyerData?.provinsi
                        val userkodepos = buyerData?.kodepos

                        findViewById<TextView>(R.id.address).text = "$useralamat, $userkota, $userprovinsi, $usernegara"
                        findViewById<TextView>(R.id.kodepos).text = userkodepos
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }

    private fun displayCheckoutItems(token: String) {
        val checkoutContainer = findViewById<LinearLayout>(R.id.cart_container)
        checkoutContainer.removeAllViews()

        val checkoutItemView = layoutInflater.inflate(R.layout.payment_products, checkoutContainer, false)
        checkoutContainer.addView(checkoutItemView) // Ensure the view is added to the container

        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productQty = intent.getIntExtra("productQty", 0)
        val productId = intent.getLongExtra("productId", 0)

        Log.d("IntentData", "Product Name: $productName, Image: $productImage, Price: $productPrice, Quantity: $productQty")

        if (productName != null) {
            findViewById<LinearLayout>(R.id.plusminus).visibility = View.VISIBLE
        }

        var currentTotalPrice = productPrice
        checkoutItemView.findViewById<TextView>(R.id.product_name).text = productName
        val quantityTextView = checkoutItemView.findViewById<TextView>(R.id.product_quantity)
        val plusMinusQty = checkoutItemView.findViewById<TextView>(R.id.quantity)
        quantityTextView.text = "Quantity: $productQty"

        val produkPriceTextView = checkoutItemView.findViewById<TextView>(R.id.product_price)
        produkPriceTextView.text = "Rp. ${formatWithDots((productPrice).toString())}"

        val productImageView = checkoutItemView.findViewById<ImageView>(R.id.product_image)
        productImage?.let {
            if (it.isNotEmpty()) {
                loadProductImage(it, productImageView)
            }
        }

        var currentQuantity = productQty
        val singleItemPrice = if (currentQuantity > 0) productPrice / currentQuantity else 0L

        checkoutItemView.findViewById<ImageButton>(R.id.plus).setOnClickListener {
            currentQuantity++
            quantityTextView.text = "Quantity: ${currentQuantity.toString()}"
            plusMinusQty.text = currentQuantity.toString()
            updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)
            currentTotalPrice += singleItemPrice
            updateTotalPrice(currentTotalPrice)
            addToCart(token, productId.toString(), currentQuantity)
            Log.d("CartUpdate", "Increased quantity for product $productImage to $currentQuantity")
        }

        checkoutItemView.findViewById<ImageButton>(R.id.minus).setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                quantityTextView.text = "Quantity: ${currentQuantity.toString()}"
                plusMinusQty.text = currentQuantity.toString()
                updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)
                currentTotalPrice -= singleItemPrice
                updateTotalPrice(currentTotalPrice)
                addToCart(token, productId.toString(), currentQuantity)
                Log.d("CartUpdate", "Decreased quantity for product $productImage to $currentQuantity")
            }
        }

        updateTotalPrice(currentTotalPrice)
    }


    private fun setupBuyButton(token: String) {
        val buyButton = findViewById<MaterialButton>(R.id.buy)
        val methodPayment = intent.getStringExtra("method")
        Log.d("methodpayment", "$methodPayment")
        buyButton.setOnClickListener {
            if (cartIds.isNotEmpty() && methodPayment != null) {
                Log.d("cartIds", "$cartIds")
                addOrder(token, cartIds, methodPayment)
            } else {
                Toast.makeText(this, "Please select the payment method", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addOrder(token: String, cartId: List<Long>, metodePembayaran: String) {
        val orderRequest = InsertOrder(cartId, metodePembayaran)
        RetrofitClient.instance.createOrder(token, orderRequest).enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful) {
                    response.body()?.order?.let { order ->
                        // Navigate to ConfirmPaymentActivity with order details
                        val intent = Intent(this@BuyNowActivity, ConfirmPaymentActivity::class.java).apply {
                            putExtra("orderId", order.id.toString())
                            putExtra("orderInvoice", order.invoice)
                        }
                        startActivity(intent)
                        Toast.makeText(this@BuyNowActivity, "Order created successfully!", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Log.e("OrderError", "Null order in response.")
                    }
                } else {
                    Log.e("OrderError", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Log.e("OrderError", "Failed to create order: ${t.message}")
            }
        })
    }


    private fun addToCart(token: String, productId: String, totalBarang: Int = 1) {

        val cartRequest = InsertCartData(
            produk_id = productId,
            total_barang = totalBarang.toString()
        )

        val request = RetrofitClient.instance.addToCart(token, cartRequest)

        // Add Authorization header with the token
        request.enqueue(object : Callback<InsertCartResponse> {
            override fun onResponse(call: Call<InsertCartResponse>, response: Response<InsertCartResponse>) {
                if (response.isSuccessful) {
                    val cartDataList = response.body()?.data
                    Log.d("cartDataList", "$cartDataList")
                    val cartId = cartDataList?.id // Access the first cart item's ID
                    Log.d("cart", "Cart ID: $cartId")
                    if (cartId != null) {
                        cartIds.add(cartId)
                    } // Add the cart ID to the list
                } else {
                    val errorBody = response.errorBody()?.string() // Debug server errors
                    Log.e("cart", "Failed to add cart: ${response.message()}, Error: $errorBody")
                }
            }

            override fun onFailure(call: Call<InsertCartResponse>, t: Throwable) {
                Log.e("cart", "Failed to add cart: ${t.message}")
            }
        })
    }

    // Update the total price displayed at the bottom of the cart
    private fun updateTotalPrice(totalPrice: Long) {
        val grandTotalPrice = calculateTotalPrice(totalPrice)
        val totalPriceTextView = findViewById<TextView>(R.id.totalPrice)
        totalPriceTextView.text = "Rp. ${formatWithDots(grandTotalPrice.toString())}"
        findViewById<TextView>(R.id.total).text = "Rp. ${formatWithDots(grandTotalPrice.toString())}"
    }

    // Helper function to update the price of each item based on quantity
    private fun updateItemPrice(produkPriceTextView: TextView, itemPrice: Long, quantity: Int) {
        val totalItemPrice = itemPrice * quantity // Calculate total price based on quantity
        produkPriceTextView.text = "Rp. ${formatWithDots(totalItemPrice.toString())}"
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
}