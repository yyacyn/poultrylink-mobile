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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.yourapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartCompleteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cart_complete)
        val mycart = findViewById<TextView>(R.id.mycart)
        val onprocess = findViewById<TextView>(R.id.onprocess)
        val complete = findViewById<TextView>(R.id.complete)
        val backbuton = findViewById<ImageButton>(R.id.backbutton)

        window.navigationBarColor = resources.getColor(R.color.orange)

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
            finish()
        }

        onprocess.setOnClickListener {
            val intent = Intent(this, CartOnprogressActivity::class.java)
            startActivity(intent)
            finish()
        }

        complete.setOnClickListener {
            val intent = Intent(this, CartCompleteActivity::class.java)
            startActivity(intent)
            finish()
        }

        backbuton.setOnClickListener {
            finish()
        }

    }

    private fun Navigation() {
        val buttonProduk = findViewById<CardView>(R.id.home)
        val buttonMarket = findViewById<ImageButton>(R.id.market)
        val buttonProfile = findViewById<ImageButton>(R.id.profile)

        buttonProduk.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }

        buttonMarket.setOnClickListener {
            startActivity(Intent(this, LocationStoreActivity::class.java))
        }


    }

    // Retrieve the token from SharedPreferences
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

    private fun fetchOrderDetail(token: String, userId: Long) {
        RetrofitClient.instance.getOrderDetail(token)
            .enqueue(object : Callback<OrderDetailResponse> {
                override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                    if (response.isSuccessful) {
                        val orderdetail = response.body()?.data
                        val filteredOrderDetail = orderdetail?.filter { it.order_deleted == null && it.status != "yes" && it.buyer.user.id.toLong() == userId && it.status != "no" }
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

    fun setCategoryDrawable(
        kategori: TextView,
        drawableId: Int,
        drawableWidth: Int,
        drawableHeight: Int
    ) {
        val drawable = ContextCompat.getDrawable(kategori.context, drawableId)
        drawable?.setBounds(0, 0, drawableWidth, drawableHeight) // Set the size (width x height)
        kategori.setCompoundDrawables(
            drawable,  // Left drawable
            null,      // Top drawable
            null,      // Right drawable
            null       // Bottom drawable
        )
    }

    private fun displayOrders(token: String, orderItems: List<OrderDetailData>) {
        val historyContainer = findViewById<LinearLayout>(R.id.historyContainer)
        historyContainer.removeAllViews()

        for (orderItem in orderItems.sortedByDescending { it.id }) {
            val orderItemView = layoutInflater.inflate(R.layout.orderprogresscard, historyContainer, false)

            // Extract product details
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

            invoiceBtn.setOnClickListener {
                val intent = Intent(this@CartCompleteActivity, CompletePaymentActivity::class.java).apply {
                    putExtra("orderId", orderId.toString())
                }
                startActivity(intent)
            }

            orderItemView.findViewById<TextView>(R.id.orderDate).text = "#$orderId - $date"

            // Set UI data for product name, category, and price
            orderItemView.findViewById<TextView>(R.id.productName).text = productName
            val quantityTextView = orderItemView.findViewById<TextView>(R.id.productQuantity)
            quantityTextView.text = "Quantity: $quantity"

            // Display the initial calculated price for the item
            val produkPriceTextView = orderItemView.findViewById<TextView>(R.id.productTotalPrice)
            produkPriceTextView.text = "Rp. ${formatWithDots((totalPrice).toString())}"

            if (status == "retrieved") {
                statusTextView.text = "Retrieved"

                // Change stroke color, width, and text color
                val orangeColor = ContextCompat.getColor(this@CartCompleteActivity, R.color.orange)
                (statusTextView as MaterialButton).apply {
                    strokeColor = ColorStateList.valueOf(orangeColor)
                    strokeWidth = 4 // Set stroke width in pixels
                    setTextColor(orangeColor) // Change text color to orange
                }

                doneBtn.text = "Review"
                doneBtn.setOnClickListener {
                    val intent = Intent(this@CartCompleteActivity, RatingProdukActivity::class.java).apply {
                        putExtra("productCategory", orderItem.produk_kategori)
                        putExtra("productId", orderItem.produk_image.toLong())
                        putExtra("productName", orderItem.produk)
                        putExtra("productImage", orderItem.produk_image)
                        putExtra("orderId", orderId)
                    }
                    startActivity(intent)
                    // Action for review
                }
            } else {
                statusTextView.text = "Cancelled"

                // Change stroke color, width, and text color
                val redColor = ContextCompat.getColor(this@CartCompleteActivity, R.color.red)
                (statusTextView as MaterialButton).apply {
                    strokeColor = ColorStateList.valueOf(redColor)
                    strokeWidth = 4 // Set stroke width in pixels
                    setTextColor(redColor) // Change text color to red
                }

                doneBtn.text = "Buy again"
                doneBtn.setOnClickListener {
                    // Action for buy again
                }
            }



            if (productKategori == "Ayam" || productKategori == "Bebek") {
                val kategori = orderItemView.findViewById<TextView>(R.id.productCategory)
                kategori.text = "Poultry"
                setCategoryDrawable(kategori, R.drawable.kategori_chicken_transparent, 120, 120) // Width & Height in pixels
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

            // Load product image if available
            val productImageView = orderItemView.findViewById<ImageView>(R.id.productImage)
            if (productImage.isNotEmpty()) {
                loadProductImage(productImage, productImageView)
            }
            // Add the view to the container
            historyContainer.addView(orderItemView)
        }

    }
}