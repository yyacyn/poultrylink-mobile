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

        Navigation()

        val token = "Bearer ${getStoredToken()}"

        Navigation()

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
        val buttonProduk = findViewById<ImageButton>(R.id.home)
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

    fun setCategoryDrawable(
        kategori: TextView,
        drawableId: Int,
        drawableWidth: Int,
        drawableHeight: Int
    ) {
        val drawable = ContextCompat.getDrawable(kategori.context, drawableId)
        drawable?.setBounds(0, 0, drawableWidth, drawableHeight)
        kategori.setCompoundDrawables(
            drawable,
            null,
            null,
            null
        )
    }

    private fun displayOrders(token: String, orderItems: List<OrderDetailData>) {
        val historyContainer = findViewById<LinearLayout>(R.id.historyContainer)
        historyContainer.removeAllViews()

        for (orderItem in orderItems.sortedByDescending { it.id }) {
            val orderItemView = layoutInflater.inflate(R.layout.orderprogresscard, historyContainer, false)

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

            orderItemView.findViewById<TextView>(R.id.productName).text = productName
            val quantityTextView = orderItemView.findViewById<TextView>(R.id.productQuantity)
            quantityTextView.text = "Quantity: $quantity"

            val produkPriceTextView = orderItemView.findViewById<TextView>(R.id.productTotalPrice)
            produkPriceTextView.text = "Rp. ${formatWithDots((totalPrice).toString())}"

            if (status == "retrieved") {
                statusTextView.text = "Retrieved"

                val orangeColor = ContextCompat.getColor(this@CartCompleteActivity, R.color.orange)
                (statusTextView as MaterialButton).apply {
                    strokeColor = ColorStateList.valueOf(orangeColor)
                    strokeWidth = 4
                    setTextColor(orangeColor)
                }

                doneBtn.text = "Review"
                doneBtn.setOnClickListener {
                    val intent = Intent(this@CartCompleteActivity, RatingProdukActivity::class.java).apply {
                        putExtra("productCategory", orderItem.produk_kategori)
                        putExtra("productId", orderItem.produk_id)
                        putExtra("productName", orderItem.produk)
                        putExtra("productImage", orderItem.produk_image)
                        putExtra("orderId", orderId)
                    }
                    startActivity(intent)
                }
            } else {
                statusTextView.text = "Cancelled"

                val redColor = ContextCompat.getColor(this@CartCompleteActivity, R.color.red)
                (statusTextView as MaterialButton).apply {
                    strokeColor = ColorStateList.valueOf(redColor)
                    strokeWidth = 4
                    setTextColor(redColor)
                }

                doneBtn.text = "Buy again"
                doneBtn.setOnClickListener {
                    addToCartBuyNow(token, orderItem.produk_id.toString(), 1)
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

    private fun addToCartBuyNow(token: String, productId: String, totalBarang: Int = 1) {
        val cartRequest = InsertCartData(
            produk_id = productId,
            total_barang = totalBarang.toString()
        )

        val request = RetrofitClient.instance.addToCart(token, cartRequest)

        request.enqueue(object : Callback<InsertCartResponse> {
            override fun onResponse(call: Call<InsertCartResponse>, response: Response<InsertCartResponse>) {
                if (response.isSuccessful) {
                    Log.d("cart", "Cart added successfully: ${response.body()}")

                    val cartId = response.body()?.data?.id

                    if (cartId != null) {
                        val productName = intent.getStringExtra("productName")
                        val productImage = intent.getStringExtra("productImage")
                        val productPrice = intent.getLongExtra("productPrice", 0)
                        val product_id = intent.getLongExtra("product_id", 0)

                        val intent = Intent(this@CartCompleteActivity, BuyNowActivity::class.java).apply {
                            putExtra("productName", productName)
                            putExtra("productImage", productImage)
                            putExtra("productPrice", productPrice)
                            putExtra("productQty", 1)
                            putExtra("productId", product_id)
                            putExtra("cartId", cartId)
                        }
                        Log.d("cartIdreal", "$cartId")
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@CartCompleteActivity, "Failed to get cart ID", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@CartCompleteActivity, "Failed to add to cart: ${response.message()}", Toast.LENGTH_SHORT).show()
                    Log.e("cart", "Failed to add cart: ${response.message()}, Error: $errorBody")
                }
            }

            override fun onFailure(call: Call<InsertCartResponse>, t: Throwable) {
                Log.e("cart", "Failed to add cart: ${t.message}")
                Toast.makeText(this@CartCompleteActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}