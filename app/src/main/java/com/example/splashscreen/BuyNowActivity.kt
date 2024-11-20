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
import androidx.appcompat.app.AlertDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BuyNowActivity : AppCompatActivity() {

    private var cartIds = mutableListOf<Long>()
    private val shippingFee: Long = 300000
    private var currentQuantity: Int = 0

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
        currentQuantity = intent.getIntExtra("productQty", 0)
        val productId = intent.getLongExtra("productId", 0)
        val cartId = intent.getLongExtra("cartId", 0)

        val estDeliveryTextView = findViewById<TextView>(R.id.estDelivery)

        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, 7)

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        estDeliveryTextView.text = "Estimated delivery: $formattedDate"


        findViewById<TextView>(R.id.quantity).text = currentQuantity.toString()

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

            val imageResourceId = resources.getIdentifier(imagePayment, "drawable", packageName)
            if (imageResourceId != 0) { // Check if the resource exists
                findViewById<ImageView>(R.id.paymentIcon).setImageResource(imageResourceId)
            } else {
                findViewById<ImageView>(R.id.paymentIcon).setImageResource(R.drawable.paymenticon)
            }
        }

        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                getProfile(token)
                displayCheckoutItems(token)
                findViewById<ImageButton>(R.id.backbutton).setOnClickListener {
                    val alertDialog = AlertDialog.Builder(this)
                        .setTitle("Confirm checkout")
                        .setMessage("Are you sure you want to cancel checkout? All progress will be discarded")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, which ->
                            deleteCart(token, productId.toInt(), userId.toInt())
                        }
                        .setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }
                    alertDialog.show()
                }
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
                putExtra("productQty", currentQuantity)
                putExtra("productId", productId)
                putExtra("cartId", cartId)
            }
            startActivity(intent)
        }
        setupBuyButton(token)

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
                        Toast.makeText(this@BuyNowActivity, "Checkout cancelled", Toast.LENGTH_SHORT).show()
                        Log.d("DeleteCart", "Item deleted successfully")

                        // Navigate to ConfirmPaymentActivity with order details
                        val intent = Intent(this@BuyNowActivity, DashboardActivity::class.java)
                        startActivity(intent)

                    } else {
                        Log.e("DeleteCart", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<DeleteCartResponse>, t: Throwable) {
                    Log.e("DeleteCart", "Network Error: ${t.message}")
                }
            })
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }


    private fun calculateTotalPrice(subTotal: Long): Long {

        findViewById<TextView>(R.id.subTotal).text = "Rp. ${formatWithDots(subTotal.toString())}"

        val totalPrice = subTotal + shippingFee
        return totalPrice
    }

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
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                }
            })
    }

    private fun displayCheckoutItems(token: String) {
        val checkoutContainer = findViewById<LinearLayout>(R.id.cart_container)
        checkoutContainer.removeAllViews()

        val checkoutItemView = layoutInflater.inflate(R.layout.payment_products, checkoutContainer, false)
        checkoutContainer.addView(checkoutItemView)

        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productPrice = intent.getLongExtra("productPrice", 0)
        currentQuantity = intent.getIntExtra("productQty", 0)
        val productId = intent.getLongExtra("productId", 0)

        Log.d("IntentData", "Product Name: $productName, Image: $productImage, Price: $productPrice, Quantity: $currentQuantity")

        if (productName != null) {
            findViewById<LinearLayout>(R.id.plusminus).visibility = View.VISIBLE
        }

        val estDeliveryTextView = findViewById<TextView>(R.id.estDelivery)

        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, 7)

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        estDeliveryTextView.text = "Estimated delivery: $formattedDate"

        var currentTotalPrice = productPrice
        checkoutItemView.findViewById<TextView>(R.id.product_name).text = productName
        val quantityTextView = checkoutItemView.findViewById<TextView>(R.id.product_quantity)
        val plusMinusQty = checkoutItemView.findViewById<TextView>(R.id.quantity)
        quantityTextView.text = "Quantity: $currentQuantity"

        plusMinusQty.text = currentQuantity.toString()

        val produkPriceTextView = checkoutItemView.findViewById<TextView>(R.id.product_price)
        produkPriceTextView.text = "Rp. ${formatWithDots((productPrice).toString())}"

        val productImageView = checkoutItemView.findViewById<ImageView>(R.id.product_image)
        productImage?.let {
            if (it.isNotEmpty()) {
                loadProductImage(it, productImageView)
            }
        }

        val singleItemPrice = if (currentQuantity > 0) productPrice / currentQuantity else 0L
        val methodPayment = intent.getStringExtra("method")

        checkoutItemView.findViewById<ImageButton>(R.id.plus).setOnClickListener {
            currentQuantity++
            quantityTextView.text = "Quantity: ${currentQuantity.toString()}"
            plusMinusQty.text = currentQuantity.toString()
            updateItemPrice(produkPriceTextView, singleItemPrice, currentQuantity)
            currentTotalPrice += singleItemPrice
            updateTotalPrice(currentTotalPrice)
            updateCartItemsForCheckout(token, cartIds[0], currentQuantity)
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
                updateCartItemsForCheckout(token, cartIds[0], currentQuantity)
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

    private fun updateTotalPrice(totalPrice: Long) {
        val grandTotalPrice = calculateTotalPrice(totalPrice)
        val totalPriceTextView = findViewById<TextView>(R.id.totalPrice)
        totalPriceTextView.text = "Rp. ${formatWithDots(grandTotalPrice.toString())}"
        findViewById<TextView>(R.id.total).text = "Rp. ${formatWithDots(grandTotalPrice.toString())}"
    }

    private fun updateItemPrice(produkPriceTextView: TextView, itemPrice: Long, quantity: Int) {
        val totalItemPrice = itemPrice * quantity
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
                        }
                    } else {
                        Log.e("FetchCarts", "Error: ${response.code()}")
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {
                    Log.e("FetchCarts", "Network Error: ${t.message}")
                    callback(null)
                }
            })
    }
}