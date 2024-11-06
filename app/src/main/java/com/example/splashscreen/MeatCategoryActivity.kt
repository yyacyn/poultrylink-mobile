package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
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
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MeatCategoryActivity : AppCompatActivity() {
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    // store the list of all products for filtering
    private var allProducts: List<Products> = listOf()

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

        // initialize the search input
        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterProducts(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        lifecycleScope.launch {
            if (categoryIds != null) {
                fetchProductsByCategory(categoryIds)
            }
        }
    }

    // price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
    }

    // display all products with poutlry as the category into grid
    private fun displayProductsInGrid(products: List<Products>) {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        gridLayout.removeAllViews()


        for ((index, product) in products.withIndex()) {
            val cardView = layoutInflater.inflate(R.layout.product_card, gridLayout, false)
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)
            val productLocation = cardView.findViewById<TextView>(R.id.productLocation)

            val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/${product.image}/1.jpg"
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.emiya)
                .error(R.drawable.sekar)
                .into(productImage)

            productName.text = product.nama_produk

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val ratingData = fetchProductRating(product.id)
                    if (ratingData != null) {
                        val (averageRating, totalReviews) = ratingData
                        productRating.text = averageRating.toString()
                        productAmountRating.text = "($totalReviews Reviews)"
                    } else {
                        productRating.text = "0.0"
                        productAmountRating.text = "(0 Reviews)"
                    }
                } catch (e: Exception) {
                    Log.e("displayProducts", "Error fetching rating: ${e.message}")
                    productRating.text = "N/A"
                    productAmountRating.text = "(N/A Reviews)"
                }
            }

            val value = formatWithDots(product.harga)
            productPrice.text = "Rp. $value"

            val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(if (index % 2 == 0) 30 else 20, 20, 10, 20)

            cardView.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val ratingData = fetchProductRating(product.id)
                        val averageRating = ratingData?.first ?: 0.0
                        val totalReviews = ratingData?.second ?: 0

                        val intent = Intent(this@MeatCategoryActivity, ProdukActivity::class.java).apply {
                            putExtra("product_id", product.id)
                            putExtra("productName", product.nama_produk)
                            putExtra("productImage", product.image)
                            putExtra("productRating", averageRating.toFloat())
                            putExtra("productTotalReviews", totalReviews)
                            putExtra("productPrice", product.harga)
                            putExtra("productDesc", product.deskripsi)
                            putExtra("supplierId", product.supplier_id)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("displayProducts", "Error passing data: ${e.message}")
                    }
                }
            }
            cardView.layoutParams = params
            gridLayout.addView(cardView)
        }
    }

    //get each products avg rating and total review
    suspend fun fetchProductRating(productId: Long): Pair<Double, Int>? {
        return try {
            val response = RetrofitClient.instance.getProductRating(mapOf("product_id" to productId))

            Log.d("fetchProductRating", "Response body: ${response.body()}")

            if (response.isSuccessful) {
                val data = response.body()

                if (data != null && data is List<*>) {
                    val firstItem = data.firstOrNull() as? Map<String, Any>
                    if (firstItem != null) {
                        var averageRating = (firstItem["average_rating"] as? Double) ?: 0.0
                        averageRating = String.format("%.2f", averageRating).toDouble()

                        val totalReviews = (firstItem["total_reviews"] as? Double)?.toInt() ?: 0
                        return Pair(averageRating, totalReviews)
                    }
                }
                Pair(0.0, 0)
            } else {
                Log.e("fetchProductRating", "Response not successful: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("fetchProductRating", "Error fetching rating: ${e.message}")
            null
        }
    }

    // get products by their category
    private suspend fun fetchProductsByCategory(categoryIds: String) {
        val formattedCategoryIds = "{$categoryIds}"
        val requestBody = mapOf("category_ids" to formattedCategoryIds)
        val response = RetrofitClient.instance.getProductsByCategory(requestBody)

        if (response.isSuccessful) {
            allProducts = response.body() ?: listOf()
            displayProductsInGrid(allProducts)
        } else {
            Log.e("fetchProducts", "Error fetching products: ${response.errorBody()?.string()}")
        }
    }

    // filter products by search input
    private fun filterProducts(query: String) {
        val filteredProducts = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.nama_produk.contains(query, ignoreCase = true) }
        }
        displayProductsInGrid(filteredProducts)
    }
}