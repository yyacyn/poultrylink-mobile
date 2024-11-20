package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class SearchProdukActivity : AppCompatActivity() {

    private var allProducts: List<ProductData> = listOf()
    private var searchQuery: String? = null
    private lateinit var gridLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_produk)

        searchQuery = intent.getStringExtra("search_query")?.trim()
        gridLayout = findViewById(R.id.gridLayout)

        val token = "Bearer " + intent.getStringExtra("TOKEN")

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.pemasok).setOnClickListener {
            startActivity(Intent(this, SearchPemasokActivity::class.java))
            finish()
        }

        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchQuery?.let { searchInput.setText(it) }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadProducts(token)
    }



    private fun loadProducts(token: String) {
        // Make API call to get products and set `allProducts`
        RetrofitClient.instance.getProducts("$token").enqueue(object : Callback<ProductResponse> {
            override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                if (response.isSuccessful) {
                    allProducts = response.body()?.data ?: emptyList()
                    getReviews(token) // Fetch reviews once products are loaded
                }
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                // Handle error
            }
        })
    }

    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
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

    private fun displayProducts(products: List<ProductData>, reviews: List<ReviewData>) {
        gridLayout.removeAllViews()
        val sortedProducts = products.sortedByDescending { it.created_at }
        for ((index, product) in sortedProducts.withIndex()) {
            val cardView = layoutInflater.inflate(R.layout.product_card, gridLayout, false)
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)

            loadProductImage(product.image, productImage)

            productName.text = product.nama_produk

            // Filter reviews for the current product
            val productReviews = reviews.filter { it.produk_id == product.id.toString() }
            val averageRating = if (productReviews.isNotEmpty()) {
                productReviews.map { it.rating }.average()
            } else {
                0.0
            }
            val totalReviews = productReviews.size
            productRating.text = "%.1f".format(averageRating)
            productAmountRating.text = "(${productReviews.size} Reviews)"

            productPrice.text = "Rp. ${formatWithDots(product.harga.toLong())}"

            val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(if (index % 2 == 0) 30 else 20, 20, 10, 20)
            cardView.layoutParams = params

            cardView.setOnClickListener {
                val intent = Intent(this@SearchProdukActivity, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.id.toString())
                    putExtra("productRating", "%.1f".format(averageRating).toFloat())
                    putExtra("productTotalReviews", totalReviews)
                    putExtra("productPrice", product.harga.toLong())
                    putExtra("productDesc", product.deskripsi)
                    putExtra("supplierId", product.supplier_id)
                    putExtra("productQty", product.jumlah)
                    product.supplier?.buyer?.let { it1 -> putExtra("supplierImage", it1.id) }
                    putExtra("supplierKota", product.supplier?.kota)
                    putExtra("supplierNegara", product.supplier?.negara)
                    putExtra("supplierPronvisi", product.supplier?.provinsi)
                    putExtra("supplierName", product.supplier?.nama_toko)
                    putExtra("supplierRating", product.supplier?.rating)
                    putExtra("productCategory", product.kategori_id)
                }
                startActivity(intent)
            }
            gridLayout.addView(cardView)
        }
    }

    private fun getReviews(token: String) {
        RetrofitClient.instance.getReviews("$token").enqueue(object : Callback<ReviewResponse> {
            override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
                if (response.isSuccessful) {
                    val reviews = response.body()?.data ?: emptyList()
                    filterProducts(searchQuery.orEmpty(), reviews) // Initial display
                }
            }

            override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
                // Handle network errors
            }
        })
    }

    private fun filterProducts(query: String, reviews: List<ReviewData> = emptyList()) {
        val filteredProducts = if (query.isEmpty()) {
            allProducts }
        else {
            allProducts.filter {
                it.nama_produk.contains(query, ignoreCase = true) || it.kategori.contains(query, ignoreCase = true)
            }
        }
        displayProducts(filteredProducts, reviews)
    }

}