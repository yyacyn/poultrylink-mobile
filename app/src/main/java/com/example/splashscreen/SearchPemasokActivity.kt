package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.api.Distribution.BucketOptions.Linear
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class SearchPemasokActivity : AppCompatActivity() {

    private var allSuppliers: List<SupplierData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.search_pemasok)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val token = "Bearer ${getStoredToken()}"

        val supplierName = findViewById<TextView>(R.id.supplierName)
        val supplierLocation = findViewById<TextView>(R.id.supplierLocation)
        val supplierImage = findViewById<CircleImageView>(R.id.supplierImage)
        val supplierProductContainer = findViewById<LinearLayout>(R.id.productContainer)
        val supplierContainer = findViewById<LinearLayout>(R.id.supplierContainer)

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }


        findViewById<TextView>(R.id.Produk).setOnClickListener {
            startActivity(Intent(this, SearchProdukActivity::class.java))
            finish()
        }

        getSupplier(token)

        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSuppliers(token, s.toString().trim())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    // load user's avatar from supabase
    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView) {
        lifecycleScope.launch {
            try {
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath/1.jpg?t=${System.currentTimeMillis()}"


                // Use Glide to load the image into the ImageView
                Glide.with(this@SearchPemasokActivity)
                    .load(imageUrl)
                    .override(50, 50)
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(imageView)
                Log.d("ImageLoadSuppplier", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }

    private fun getSupplier(token: String) {
        RetrofitClient.instance.getSupplier(token)
            .enqueue(object : Callback<SupplierResponse> {
                override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                    if (response.isSuccessful) {
                        val supplierData = response.body()?.data ?: emptyList()
                        Log.d("SupplierResponse", "Suppliers: $supplierData")
                        allSuppliers = supplierData
                        // Get all products and reviews at once
                        getAllProductsAndReviews(token, supplierData)
                    } else {
                        Log.e("SupplierError", "Failed to fetch supplier data")
                    }
                }

                override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                    Log.e("SupplierError", "Error: ${t.message}")
                }
            })
    }

    private fun getAllProductsAndReviews(token: String, suppliers: List<SupplierData>) {
        // First, get all products
        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        val products = response.body()?.data ?: emptyList()
                        val filteredProducts = products.sortedByDescending { it.id }
                        // Then get all reviews
                        getReviewsForAllProducts(token, filteredProducts, suppliers)
                    } else {
                        Log.e("ProductError", "Failed to fetch products")
                    }
                }
                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    Log.e("ProductError", "Error: ${t.message}")
                }
            })
    }

    private fun displaySupplierProducts(products: List<ProductData>, reviews: List<ReviewData>, productContainer: LinearLayout) {
        productContainer.removeAllViews()

        for (product in products.take(3)) {
            val productView = layoutInflater.inflate(R.layout.supplier_products, productContainer, false)

            val productImage = productView.findViewById<ImageView>(R.id.supplierProductImage)
            val productPrice = productView.findViewById<TextView>(R.id.supplierProductPrice)

            loadProductImage(product.image, productImage)

            // Calculate product rating
            val productReviews = reviews.filter { it.produk_id == product.id.toString() }
            val averageRating = productReviews.takeIf { it.isNotEmpty() }
                ?.map { it.rating }
                ?.average() ?: 0.0

            // Format and set price
            val formattedPrice = formatWithDots(product.harga.toLong())
            productPrice.text = "Rp. $formattedPrice"

            // Set click listener for product
            productView.setOnClickListener {
                val intent = Intent(this@SearchPemasokActivity, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.image)
                    putExtra("productRating", "%.1f".format(averageRating).toFloat())
                    putExtra("productTotalReviews", productReviews.size)
                    putExtra("productPrice", product.harga.toLong())
                    putExtra("productDesc", product.deskripsi)
                    putExtra("productQty", product.jumlah)
                    putExtra("supplierId", product.supplier_id)
                    putExtra("supplierKota", product.supplier?.kota)
                    putExtra("supplierNegara", product.supplier?.negara)
                    putExtra("supplierToko", product.supplier?.nama_toko)
                    putExtra("productCategory", product.kategori_id)
                }
                startActivity(intent)
            }

            productContainer.addView(productView)
        }
    }

    // price formatting
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
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .transition(DrawableTransitionOptions.withCrossFade())
                .priority(Priority.HIGH)
                .error(R.drawable.sekar)
                .into(imageView)
            Log.d("loadingimage","loading image from: $imageUrl")
        } catch (e: Exception) {
            Log.e("ImageLoadError", "Failed to load product image: ${e.message}")
        }
    }

    private fun getReviewsForAllProducts(token: String, products: List<ProductData>, suppliers: List<SupplierData>) {
        RetrofitClient.instance.getReviews(token)
            .enqueue(object : Callback<ReviewResponse> {
                override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
                    if (response.isSuccessful) {
                        val reviews = response.body()?.data ?: emptyList()
                        // Now we have all data, display suppliers with their products
                        displaySuppliersWithProducts(suppliers, products, reviews)
                    } else {
                        Log.e("ReviewError", "Failed to fetch reviews")
                    }
                }
                override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
                    Log.e("ReviewError", "Error: ${t.message}")
                }
            })
    }

    private fun displaySuppliersWithProducts(suppliers: List<SupplierData>, allProducts: List<ProductData>, allReviews: List<ReviewData>) {
        val supplierContainer = findViewById<LinearLayout>(R.id.supplierContainer)
        supplierContainer.removeAllViews()

        for (supplier in suppliers) {
            // Inflate supplier card view
            val supplierView = layoutInflater.inflate(R.layout.supplier_card, supplierContainer, false)

            // Set up supplier details
            val supplierImage = supplierView.findViewById<CircleImageView>(R.id.supplierImage)
            val supplierName = supplierView.findViewById<TextView>(R.id.supplierName)
            val supplierLocation = supplierView.findViewById<TextView>(R.id.supplierLocation)
            val productContainer = supplierView.findViewById<LinearLayout>(R.id.productContainer)

            // Set supplier details
            supplierName.text = supplier.nama_toko
            supplierLocation.text = "${supplier.kota}, ${supplier.negara}"
            loadImageFromSupabase(supplier.buyer?.id.toString(), supplierImage)

            val lihatToko = supplierView.findViewById<Button>(R.id.lihatToko)

            // Filter and display products for this supplier
            val supplierProducts = allProducts.filter { it.supplier_id == supplier.id.toString() }
            displaySupplierProducts(supplierProducts, allReviews, productContainer)

            // Set click listener for supplier card
            lihatToko.setOnClickListener {
                val intent = Intent(this@SearchPemasokActivity, TokoActivity::class.java).apply {
                    putExtra("supplier_id", supplier.id)
                    putExtra("supplierName", supplier.nama_toko)
                    putExtra("supplierKota", supplier.kota)
                    putExtra("supplierNegara", supplier.negara)
                    putExtra("supplierProvinsi", supplier.provinsi)
                    putExtra("supplierImage", supplier.buyer?.id.toString())
                }
                startActivity(intent)
            }

            supplierContainer.addView(supplierView)
        }
    }

    private fun filterSuppliers(token: String, query: String) {
        val filteredSuppliers = if (query.isEmpty()) {
            allSuppliers
        } else {
            allSuppliers.filter {
                it.nama_toko?.contains(query, ignoreCase = true) == true
            }
        }

        Log.d("FilteredSuppliers", "Filtered: $filteredSuppliers, Query: $query")
        getAllProductsAndReviews(token, filteredSuppliers)
    }


}