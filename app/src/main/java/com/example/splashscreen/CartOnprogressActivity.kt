package com.example.splashscreen

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.gson.Gson
import com.yourapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartOnprogressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cart_onprogress)
        val mycart = findViewById<TextView>(R.id.mycart)
        val onprocess = findViewById<TextView>(R.id.onprocess)
        val complete = findViewById<TextView>(R.id.complete)
        val backbtn = findViewById<ImageButton>(R.id.backbtn)

        val token = "Bearer ${getStoredToken()}"

        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                fetchOrderDetail(token, userId)
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }

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

        backbtn.setOnClickListener {
            finish()
        }
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

    private fun fetchOrderDetail(token: String, userId: Long) {
        RetrofitClient.instance.getOrderDetail(token)
            .enqueue(object : Callback<OrderDetailResponse> {
                override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                    if (response.isSuccessful) {
                        val orderdetail = response.body()?.data
                        val filteredOrderDetail = orderdetail?.filter { it.order_deleted == null && it.status != "cancelled" && it.buyer.user.id.toLong() == userId && it.status != "retrieved" }
                        if (filteredOrderDetail != null) {
                            displayOrders(token, filteredOrderDetail)
                        }
                        Log.d("FetchOrderDetail", "Filtered Order Detail: $filteredOrderDetail")
                    } else {
                        Log.e("FetchOrderDetail", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                    Log.e("FetchOrderDetail", "Network Error: ${t.message}")
                }
            })
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

    private fun displayOrders(token: String, orderItems: List<OrderDetailData>) {
        val orderContainer = findViewById<LinearLayout>(R.id.orderContainer)
        orderContainer.removeAllViews()

        for (orderItem in orderItems.sortedByDescending { it.id }) {
            val orderItemView = layoutInflater.inflate(R.layout.orderprogresscard, orderContainer, false)

            val productName = orderItem.produk
            val productImage = orderItem.produk_image
            val productKategori = orderItem.produk_kategori
            val quantity = orderItem.quantity
            val totalPrice = orderItem.total_price
            val status = orderItem.status
            val date = orderItem.tanggal
            val orderId = orderItem.order_id

            val statusTextView = orderItemView.findViewById<TextView>(R.id.status)
            val doneBtn = orderItemView.findViewById<Button>(R.id.doneBtn)

            val invoiceBtn = orderItemView.findViewById<Button>(R.id.invoiceBtn)

            orderItemView.findViewById<TextView>(R.id.orderDate).text = "#$orderId - $date"

            orderItemView.findViewById<TextView>(R.id.productName).text = productName
            val quantityTextView = orderItemView.findViewById<TextView>(R.id.productQuantity)
            quantityTextView.text = "Quantity: $quantity"

            val produkPriceTextView = orderItemView.findViewById<TextView>(R.id.productTotalPrice)
            produkPriceTextView.text = "Rp. ${formatWithDots((totalPrice).toString())}"

            val invoiceButton = orderItemView.findViewById<Button>(R.id.invoiceBtn)

            if (status == "no" ){
                statusTextView.text = "Unpaid"
                doneBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this@CartOnprogressActivity, R.color.red)))
                invoiceBtn.text = "Pay"
                invoiceBtn.setOnClickListener {
                    confirmOrder(token, orderId.toInt())
                }
                doneBtn.text = "Cancel"
                doneBtn.setOnClickListener {
                    val alertDialog = AlertDialog.Builder(this)
                        .setTitle("Confirm cancel order")
                        .setMessage("Are you sure you want to cancel this order? Cancelling this order will cancel the same product within the same order?")
                        .setCancelable(false) // Set to false so user must choose either Yes or No
                        .setPositiveButton("Yes") { dialog, which ->
                            cancelOrder(token, orderId.toInt(), productImage.toInt())
                            getUser(token) { userId ->
                                if (userId != null) {
                                    Log.d("MainActivity", "Fetched User ID: $userId")
                                    fetchOrderDetail(token, userId)
                                } else {
                                    Log.e("MainActivity", "Failed to fetch User ID")
                                }
                            }
                        }
                        .setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }
                    alertDialog.show()
                }
            } else if (status == "yes") {
                statusTextView.text = "Arrived"
                doneBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this@CartOnprogressActivity, R.color.orange)))
                doneBtn.text = "Done"
                doneBtn.setOnClickListener {
                    val alertDialog = AlertDialog.Builder(this)
                        .setTitle("Confirm finish order")
                        .setMessage("Are you sure you want to finish and retrieve this order?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, which ->
                            retrieveOrder(token, orderId.toInt(), productImage.toInt())
                            getUser(token) { userId ->
                                if (userId != null) {
                                    Log.d("MainActivity", "Fetched User ID: $userId")
                                    fetchOrderDetail(token, userId)
                                } else {
                                    Log.e("MainActivity", "Failed to fetch User ID")
                                }
                            }
                        }
                        .setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }
                    alertDialog.show()
                }

                invoiceBtn.setOnClickListener {
                    val intent = Intent(this@CartOnprogressActivity, CompletePaymentActivity::class.java).apply {
                        putExtra("orderId", orderId.toString())
                    }
                    startActivity(intent)
                }
            }
            else {
                statusTextView.text = "On progress"
                doneBtn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this@CartOnprogressActivity, R.color.red)))
                doneBtn.text = "Cancel"
                doneBtn.setOnClickListener {
                    val alertDialog = AlertDialog.Builder(this)
                        .setTitle("Confirm cancel order")
                        .setMessage("Are you sure you want to cancel this order? Cancelling this order will cancel the same product within the same order")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, which ->
                            cancelOrder(token, orderId.toInt(), productImage.toInt())
                            getUser(token) { userId ->
                                if (userId != null) {
                                    Log.d("MainActivity", "Fetched User ID: $userId")
                                    fetchOrderDetail(token, userId)
                                } else {
                                    Log.e("MainActivity", "Failed to fetch User ID")
                                }
                            }
                        }
                        .setNegativeButton("No") { dialog, which ->
                            dialog.dismiss()
                        }
                    alertDialog.show()
                }
            }

            if (productKategori == "Ayam" || productKategori == "Bebek") {
                val kategori = orderItemView.findViewById<TextView>(R.id.productCategory)
                kategori.text = "Poultry"
                setCategoryDrawable(kategori, R.drawable.kategori_chicken_transparent, 120, 120)
            } else if (productKategori.contains("Telur", true)) {
                val kategori = orderItemView.findViewById<TextView>(R.id.productCategory)
                kategori.text = "Egg"
                setCategoryDrawable(kategori, R.drawable.kategori_egg_transparent, 120, 120)
            } else if (productKategori.contains("Potong", true)) {
                val kategori = orderItemView.findViewById<TextView>(R.id.productCategory)
                kategori.text = "Meat"
                setCategoryDrawable(kategori, R.drawable.kategori_meat_transparent, 120, 120)
            } else {
                val kategori = orderItemView.findViewById<TextView>(R.id.productCategory)
                kategori.text = "Seed"
                setCategoryDrawable(kategori, R.drawable.kategori_egg_transparent, 120, 120)
            }

            val productImageView = orderItemView.findViewById<ImageView>(R.id.productImage)
            if (productImage.isNotEmpty()) {
                loadProductImage(productImage, productImageView)
            }
            orderContainer.addView(orderItemView)
        }
    }

    fun setCategoryDrawable(
        kategori: TextView,
        drawableId: Int,
        drawableWidth: Int,
        drawableHeight: Int
    ) {
        val drawable = ContextCompat.getDrawable(kategori.context, drawableId)
        drawable?.setBounds(0, 0, drawableWidth, drawableHeight) // Set the size (width x height)
        kategori.setCompoundDrawables(
            drawable,
            null,
            null,
            null
        )
    }

    private fun cancelOrder(token: String, orderId: Int, productImage: Int) {
        Log.d("cancelOrder", "orderId: $orderId, productImage: $productImage")

        val request = CancelOrderRequest(orderId)

        val gson = Gson()
        val jsonRequest = gson.toJson(request)
        Log.d("cancelOrder", "Request JSON: $jsonRequest")

        RetrofitClient.instance.cancelOrder(token, request)
            .enqueue(object : Callback<CancelOrderResponse> {
                override fun onResponse(call: Call<CancelOrderResponse>, response: Response<CancelOrderResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CartOnprogressActivity, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                        Log.d("CancelOrder", "Order cancelled successfully")
                    } else {
                        Log.e("CancelOrder", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<CancelOrderResponse>, t: Throwable) {
                    Log.e("CancelOrder", "Network Error: ${t.message}")
                }
            })
    }

    private fun confirmOrder(token: String, orderId: Int) {
        val orderRequest = RetrieveOrderRequest(orderId)
        RetrofitClient.instance.confirmOrder(token, orderRequest).enqueue(object :
            Callback<CancelOrderResponse> {
            override fun onResponse(call: Call<CancelOrderResponse>, response: Response<CancelOrderResponse>) {
                if (response.isSuccessful) {
                    val orderResponse = response.body()
                    val intent = Intent(this@CartOnprogressActivity, ConfirmPaymentActivity::class.java).apply {
                        if (orderResponse != null) {
                            putExtra("orderId", orderResponse.order
                                ?.id.toString())
                            putExtra("orderInvoice", orderResponse.order?.invoice.toString())
                        }
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("OrderError", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<CancelOrderResponse>, t: Throwable) {
                Log.e("OrderError", "Failed to create order: ${t.message}")
            }
        })
    }


    private fun retrieveOrder(token: String, orderId: Int, productImage: Int) {
        val request = RetrieveOrderRequest(orderId)
        RetrofitClient.instance.retrieveOrder(token, request)
            .enqueue(object : Callback<OrderDetailResponse> {
                override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CartOnprogressActivity, "Order retrieved successfully", Toast.LENGTH_SHORT).show()
                        Log.d("OrderRetrieve", "Order retrieved successfully")
                    } else {
                        Log.e("OrderRetrieve", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                    Log.e("OrderRetrieve", "Network Error: ${t.message}")
                }
            })
    }

}