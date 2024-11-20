package com.example.splashscreen

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class TokoActivity : AppCompatActivity() {

    private var diikuti: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.toko)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val token = "Bearer ${getStoredToken()}"

        val supplierId = intent.getStringExtra("supplierId")
        val supplierName = intent.getStringExtra("supplierName")
        val supplierKota = intent.getStringExtra("supplierKota")
        val supplierNegara = intent.getStringExtra("supplierNegara")
        val supplierProvinsi = intent.getStringExtra("supplierProvinsi")
        val supplierImage = intent.getStringExtra("supplierImage")
        val supplierRating = intent.getStringExtra("supplierRating")
        Log.d("supplierId", "$supplierId")
        Log.d("supplierImage", "$supplierImage")

        val ikutiSupplier = findViewById<Button>(R.id.ikutiSupplier)

        ikutiSupplier.setOnClickListener {
            if (diikuti == true) {
                ikutiSupplier.text = "+ Follow"
                diikuti = false
            } else {
                ikutiSupplier.text = "Followed"
                diikuti = true
            }
        }

        val storeNameTextView = findViewById<TextView>(R.id.store_name)
        storeNameTextView.text = supplierName

        val storeLocationTextView = findViewById<TextView>(R.id.store_location)
        storeLocationTextView.text = supplierKota + ", " + supplierProvinsi + ", " + supplierNegara

//        val storeRatingTextView = findViewById<TextView>(R.id.rating_text)
//        storeRatingTextView.text = supplierRating

        val storeImageView = findViewById<CircleImageView>(R.id.user_pfp)
        loadImageFromSupabase(supplierImage.toString(), storeImageView)

        val backButton = findViewById<ImageButton>(R.id.back_btn)
        backButton.setOnClickListener {
            finish()
        }

        if (supplierId != null) {
            getProducts(token, supplierId)
        }

        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {

                val query = searchInput.text.toString().trim()
                val intent = Intent(this@TokoActivity, SearchProdukActivity::class.java).apply {
                    putExtra("search_query", query)
                    putExtra("TOKEN", token)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

//        findViewById<Button>(R.id.chatSupplier).setOnClickListener {
//            val intent = Intent(this@TokoActivity, Lifechat2Activity::class.java).apply {
//                putExtra("receiverName", supplierName)
//                putExtra("receiverImage", supplierImage)
//            }
//            startActivity(intent)
//        }
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView) {
        lifecycleScope.launch {
            try {
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath/1.jpg?t=${System.currentTimeMillis()}"

                Glide.with(this@TokoActivity)
                    .load(imageUrl)
                    .override(100, 100)
                    .placeholder(R.drawable.fotoprofil)
                    .error(R.drawable.fotoprofil)
                    .into(imageView)
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
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

    private fun displayRecentProducts(products: List<ProductData>, reviews: List<ReviewData>) {
        val sortedProducts = products.sortedByDescending { it.id }
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
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
                val intent = Intent(this@TokoActivity, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.id.toString())
                    putExtra("productRating", "%.1f".format(averageRating).toFloat())
                    putExtra("productTotalReviews", totalReviews)
                    putExtra("productPrice", product.harga.toLong())
                    putExtra("productDesc", product.deskripsi)
                    putExtra("productQty", product.jumlah)
                    putExtra("supplierId", product.supplier_id)
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
            cardView.layoutParams = params
            gridLayout.addView(cardView)
        }
    }

    private fun displayPopularProducts(products: List<ProductData>, reviews: List<ReviewData>)
    {
        val container = findViewById<LinearLayout>(R.id.popularProductsContainer)
        container.removeAllViews()
        val sortedProducts = products.sortedByDescending { product ->
            val productReviews = reviews.filter { it.produk_id == product.id.toString() }
            productReviews.size
        }
        for ((index, product) in sortedProducts .withIndex()) {
            val cardView = layoutInflater.inflate(R.layout.product_card, container, false)
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)
            val productLocation = cardView.findViewById<TextView>(R.id.productLocation)

            loadProductImage(product.image, productImage)

            productName.text = product.nama_produk

            productLocation.text = "${product.supplier?.kota}, ${product.supplier?.negara}"

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
                val intent = Intent(this@TokoActivity, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.image)
                    putExtra("productRating", "%.1f".format(averageRating).toFloat())
                    putExtra("productTotalReviews", totalReviews)
                    putExtra("productPrice", product.harga.toLong())
                    putExtra("productDesc", product.deskripsi)
                    putExtra("productQty", product.jumlah)
                    putExtra("supplierId", product.supplier_id)
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
            cardView.layoutParams = params
            container.addView(cardView)
        }
    }

    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
    }

    private fun getReviews(token: String, products: List<ProductData>) {
        RetrofitClient.instance.getReviews(token)
            .enqueue(object : Callback<ReviewResponse> {
                override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
                    if (response.isSuccessful) {
                        val reviews = response.body()?.data ?: emptyList()
                        Log.d("fetchingreviews", "$reviews")
                        displayPopularProducts(products,reviews)
                        displayRecentProducts(products, reviews)
                    } else {
                    }
                }

                override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
                }
            })
    }

    private fun getProducts(token: String, supplierId: String) {
        Log.d("gettingproducts", "getting products")
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        val products = response.body()?.data ?: emptyList()
                        val filteredProducts = products.filter { it.supplier_id == supplierId }
                        Log.d("getproducts", "$products")
                        getReviews(token, filteredProducts)
                    } else {
                        when (response.code()) {
                            401 -> Log.e("getproducts", "Unauthorized access. Check token validity.")
                            403 -> Log.e("getproducts", "Access forbidden.")
                            404 -> Log.e("getproducts", "Products not found.")
                            else -> Log.e("getproducts", "Server error: ${response.code()} ${response.message()}")
                        }
                        displayErrorMessage(gridLayout, "Failed to load products. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    Log.e("getproducts", "Network failure: ${t.localizedMessage}", t)
                    displayErrorMessage(gridLayout, "Network error. Check your connection and try again.")
                }
            })
    }

    private fun displayErrorMessage(gridLayout: GridLayout, message: String) {
        gridLayout.removeAllViews()
        val errorTextView = TextView(gridLayout.context).apply {
            text = message
            textSize = 16f
            setTextColor(Color.RED)
            gravity = Gravity.CENTER
        }
        gridLayout.addView(errorTextView)
    }

}