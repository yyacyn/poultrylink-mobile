package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
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

class MeatCategoryActivity : AppCompatActivity() {

    // store the list of all products for filtering
    private var allProducts: List<ProductData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.meat_category)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val categoryIds = intent.getStringExtra("categoryIds") ?: ""

        val token = "Bearer ${getStoredToken()}"

        // Initialize the search input
        val searchInput = findViewById<EditText>(R.id.searchInput)
        // Set up TextWatcher for real-time filtering
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterProducts(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Set up OnEditorActionListener for Enter key
        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {

                val query = searchInput.text.toString().trim()
                val intent = Intent(this@MeatCategoryActivity, SearchProdukActivity::class.java).apply {
                    putExtra("search_query", query)
                    putExtra("TOKEN", token)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

        getProducts(token, findViewById(R.id.gridLayout))
    }

    // Price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
    }

    private fun getProducts(token: String, gridLayout: GridLayout) {
        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        // Filter products with kategori_id of 1 or 2
                        val filteredProducts = response.body()?.data?.filter {
                            it.kategori_id == "3" || it.kategori_id == "4"
                        } ?: emptyList()

                        // Store the filtered products in allProducts for search filtering
                        allProducts = filteredProducts

                        getReviews(token, filteredProducts, gridLayout)
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }

    private fun loadProductImage(imagePath: String, imageView: ImageView, forceRefresh: Boolean = false) {
        try {
            val baseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$imagePath/1.jpg"
            val imageUrl = if (forceRefresh) {
                "$baseUrl?t=${System.currentTimeMillis()}"
            } else {
                baseUrl
            }

            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.emiya)
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

    private fun displayProducts(products: List<ProductData>, reviews: List<ReviewData>, gridLayout: GridLayout) {
        // Sort products by created_at in descending order to show the newest products first
        val sortedProducts = products.sortedByDescending { it.created_at }

        gridLayout.removeAllViews()
        for ((index, product) in sortedProducts.withIndex()) {
            val cardView = layoutInflater.inflate(R.layout.product_card, gridLayout, false)
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)
            val productLocation = cardView.findViewById<TextView>(R.id.productLocation)

            loadProductImage(product.image, productImage)

            productName.text = product.nama_produk
            productLocation.text = "${product.supplier?.kota}, ${product.supplier?.negara}"

            // Filter reviews for the current product
            val productReviews = reviews.filter { it.produk_id == product.id.toString() }
            val averageRating = if (productReviews.isNotEmpty()) {
                productReviews.map { it.rating }.average()
            } else {
                0.0
            }
            val totalReviews = productReviews.size

            productRating.text = "%.1f".format(averageRating)
            productAmountRating.text = "($totalReviews Reviews)"

            val value = formatWithDots(product.harga.toLong())
            productPrice.text = "Rp. $value"

            val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
            if (index % 2 == 0) {
                params.setMargins(30, 20, 10, 20)
            } else {
                params.setMargins(20, 20, 10, 20)
            }

            cardView.setOnClickListener {
                val intent = Intent(this@MeatCategoryActivity, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.image)
                    putExtra("productRating", "%.1f".format(averageRating).toFloat())
                    putExtra("productTotalReviews", totalReviews)
                    putExtra("productPrice", product.harga.toLong())
                    putExtra("productDesc", product.deskripsi)
                    putExtra("supplierId", product.supplier_id)
                    putExtra("supplierKota", product.supplier?.kota)
                    putExtra("supplierNegara", product.supplier?.negara)
                    putExtra("supplierToko", product.supplier?.nama_toko)
                    putExtra("productCategory", product.kategori_id)
                    putExtra("location", "dashboard")
                }
                startActivity(intent)
            }

            cardView.layoutParams = params
            gridLayout.addView(cardView)
        }
    }


    private fun getReviews(token: String, products: List<ProductData>, gridLayout: GridLayout) {
        RetrofitClient.instance.getReviews(token)
            .enqueue(object : Callback<ReviewResponse> {
                override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
                    if (response.isSuccessful) {
                        val reviews = response.body()?.data ?: emptyList()
                        displayProducts(products, reviews, gridLayout)
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    // Filter products by search input
    private fun filterProducts(query: String) {
        val filteredProducts = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.nama_produk.contains(query, ignoreCase = true) }
        }
        displayProducts(filteredProducts, emptyList(), findViewById(R.id.gridLayout))
    }
}