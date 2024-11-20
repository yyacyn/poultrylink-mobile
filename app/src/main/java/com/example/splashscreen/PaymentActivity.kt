package com.example.splashscreen

import ApiService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private val shippingFee: Long = 300000
    private var cartIds = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val token = "Bearer ${getStoredToken()}"

        val methodPayment = intent.getStringExtra("method")
        val imagePayment = intent.getStringExtra("image")

        val estDeliveryTextView = findViewById<TextView>(R.id.estDelivery)

        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, 7)

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        estDeliveryTextView.text = "Estimated delivery: $formattedDate"

        if (methodPayment != null && imagePayment != null) {
            findViewById<TextView>(R.id.paymentMethod).text = methodPayment.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }


            val imageResourceId = resources.getIdentifier(imagePayment, "drawable", packageName)
            if (imageResourceId != 0) {
                findViewById<ImageView>(R.id.paymentIcon).setImageResource(imageResourceId)
            } else {

                findViewById<ImageView>(R.id.paymentIcon).setImageResource(R.drawable.paymenticon)
            }
        }

        findViewById<ImageButton>(R.id.backbutton).setOnClickListener {
            finish()
        }
        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                fetchCartItems(token, userId)
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }

        findViewById<LinearLayout>(R.id.paymentContainer).setOnClickListener {
            val intent = Intent(this, MethodPaymentActivity::class.java)
            startActivity(intent)
        }

        getProfile(token)

        setupBuyButton(token)

        findViewById<TextView>(R.id.shippingFee).text = "Rp. ${formatWithDots(shippingFee.toString())}"
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
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

    private fun fetchCartItems(token: String, userId: Long) {
        RetrofitClient.instance.getAllCarts(token)
            .enqueue(object : Callback<CartResponse> {
                override fun onResponse(call: Call<CartResponse>, response: Response<CartResponse>) {
                    if (response.isSuccessful) {
                        val carts = response.body()?.data ?: emptyList()
                        Log.d("carts", "$carts")
                        val filteredCarts = carts.filter { it.deleted_at == null && it.user_id == userId.toString() }

                        Log.d("filteredCarts", "$filteredCarts")

                        val mergedCarts = mergeCartItems(filteredCarts)

                        val totalPrice = calculateTotalPrice(mergedCarts,shippingFee)
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

    private fun mergeCartItems(cartItems: List<CartData>): List<CartData> {
        val mergedItemsMap = mutableMapOf<String, CartData>()

        for (cartItem in cartItems) {
            val key = "${cartItem.user_id}-${cartItem.produk_id}"

            if (mergedItemsMap.containsKey(key)) {
                val existingItem = mergedItemsMap[key]!!
                val mergedTotalBarang = existingItem.total_barang.toInt() + cartItem.total_barang.toInt()
                val mergedTotalHarga = existingItem.total_harga.toLong() + cartItem.total_harga.toLong()

                mergedItemsMap[key] = existingItem.copy(
                    total_barang = mergedTotalBarang.toString(),
                    total_harga = mergedTotalHarga.toString()
                )
            } else {
                mergedItemsMap[key] = cartItem
            }
        }

        return mergedItemsMap.values.toList()
    }

    private fun calculateTotalPrice(cartItems: List<CartData>, shippingFee: Long = 300000): Long {
        var subTotal: Long = 0

        for (cartItem in cartItems) {
            subTotal += (cartItem.total_harga as? String)?.toLongOrNull() ?: 0L
        }

        findViewById<TextView>(R.id.subTotal).text = "Rp. ${formatWithDots(subTotal.toString())}"

        val totalPrice = subTotal + shippingFee
        return totalPrice
    }

    private fun displayCartItems(cartItems: List<CartData>, initialTotalPrice: Long) {
        val cartContainer = findViewById<LinearLayout>(R.id.cart_container)
        cartContainer.removeAllViews()
        var subTotal = initialTotalPrice
        cartIds.clear()

        for (cartItem in cartItems) {
            val cartItemView = layoutInflater.inflate(R.layout.payment_products, cartContainer, false)

            val productName = cartItem.barang.nama_produk
            val productImage = cartItem.barang.image
            val productKategori = cartItem.barang.kategori_id
            val productId = cartItem.produk_id
            val totalBarang = cartItem.total_barang.toInt()
            val productPrice = cartItem.total_harga.toLong()
            val suppliersName = cartItem.barang.supplier?.nama_toko
            val userId = cartItem.user_id
            val cartId = cartItem.id

            cartIds.add(cartId)

            cartItemView.findViewById<TextView>(R.id.product_name).text = productName
            val quantityTextView = cartItemView.findViewById<TextView>(R.id.product_quantity)
            quantityTextView.text = "Quantity: $totalBarang"

            val produkPriceTextView = cartItemView.findViewById<TextView>(R.id.product_price)
            produkPriceTextView.text = "Rp. ${formatWithDots((productPrice).toString())}"

            val productImageView = cartItemView.findViewById<ImageView>(R.id.product_image)
            if (productImage.isNotEmpty()) {
                loadProductImage(productImage, productImageView)
            }

            cartContainer.addView(cartItemView)
        }

        val totalPrice = subTotal
        updateTotalPrice(totalPrice)
    }

    private fun addOrder(token: String, cartId: List<Long>, metodePembayaran: String) {
        val gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://poultrylink.ambatuwin.xyz/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val orderRequest = InsertOrder(
            cartId,
            metodePembayaran
        )

        val request = apiService.createOrder(token, orderRequest)
        Log.d("InsertOrderRequest", Gson().toJson(orderRequest))

        request.enqueue(object : Callback<OrderResponse> {
            override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                if (response.isSuccessful) {
                    val orderResponse = response.body()
                    if (orderResponse != null) {
                        Toast.makeText(this@PaymentActivity, "Order created successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@PaymentActivity, ConfirmPaymentActivity::class.java).apply {
                            putExtra("orderId", orderResponse.order.id.toString())
                            putExtra("orderInvoice", orderResponse.order.invoice)
                        }
                        startActivity(intent)
                        Log.d("order", "Order added successfully: $orderResponse")

                    } else {
                        Log.e("order", "Null response body")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("order", "Error response: $errorBody")
                    Toast.makeText(this@PaymentActivity, "Failed to create an order: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                Log.e("order", "Failed to add order: ${t.message}")
            }
        })
    }

    private fun setupBuyButton(token: String) {
        val buyButton = findViewById<MaterialButton>(R.id.buy)
        val methodPayment = intent.getStringExtra("method")
        buyButton.setOnClickListener {

            if (cartIds.isNotEmpty() && methodPayment != null) {
                addOrder(token, cartIds, methodPayment)

                Log.d("cartIds", "$cartIds")
            } else {
                Toast.makeText(this, "Please select the payment method", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun updateTotalPrice(totalPrice: Long) {
        val totalPriceTextView = findViewById<TextView>(R.id.totalPrice)
        totalPriceTextView.text = "Rp. ${formatWithDots(totalPrice.toString())}"
        findViewById<TextView>(R.id.total).text = "Rp. ${formatWithDots(totalPrice.toString())}"
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
}