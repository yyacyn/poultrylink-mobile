package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.media.Image
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    // store list of all products to be filtered
    private var allProducts: List<Products> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard)

        val greetUser = findViewById<TextView>(R.id.greet)
        val userPfp = findViewById<ImageView>(R.id.user_pfp)
        val userLocation = findViewById<TextView>(R.id.user_location)

        loadProducts()
        Navigation()

        window.navigationBarColor = resources.getColor(R.color.orange)

        // initialize the search input
        val searchInput = findViewById<EditText>(R.id.searchInput)
        // Set up TextWatcher for real-time filtering
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
//                filterProducts(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Set up OnEditorActionListener for Enter key
        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {

                val query = searchInput.text.toString().trim()
                val intent = Intent(this@DashboardActivity, SearchProdukActivity::class.java).apply {
                    putExtra("search_query", query)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // user greeting
        lifecycleScope.launch {
            updateUserGreeting(greetUser)

            // get user id from auth email and load avatar
            val userEmail = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
            if (userEmail != null) {
                loadImageFromSupabase("$userEmail/1.jpg")

                // Get the buyer details
                val userId = userEmail.toLong()
                val buyerDetailsResponse = RetrofitClient.instance.getBuyerDetails(mapOf("p_uid" to userId))
                if (buyerDetailsResponse.isSuccessful) {
                    val buyerDetails = buyerDetailsResponse.body()?.firstOrNull()

                    if (buyerDetails != null) {
                        // Display the country and city
                        val negara = buyerDetails.negara ?: "Not available"
                        val kota = buyerDetails.kota ?: "Not available"

                        // You can display these values in a TextView
                        findViewById<TextView>(R.id.user_location).text = "$kota, $negara"
                    }
                } else {
                    Log.e("DashboardActivity", "Failed to retrieve buyer details: ${buyerDetailsResponse.errorBody()?.string()}")
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()

        // Re-fetch buyer details
        lifecycleScope.launch {
            val userEmail = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
            if (userEmail != null) {
                // Get the buyer details again after the profile update
                val userId = userEmail.toLong()
                val buyerDetailsResponse = RetrofitClient.instance.getBuyerDetails(mapOf("p_uid" to userId))
                if (buyerDetailsResponse.isSuccessful) {
                    val buyerDetails = buyerDetailsResponse.body()?.firstOrNull()

                    if (buyerDetails != null) {
                        // Display the updated country and city
                        val negara = buyerDetails.negara ?: "Not available"
                        val kota = buyerDetails.kota ?: "Not available"
                        findViewById<TextView>(R.id.user_location).text = "$kota, $negara"
                    }
                } else {
                    Log.e("ProfilActivity", "Failed to retrieve buyer details: ${buyerDetailsResponse.errorBody()?.string()}")
                }
            }
        }
    }

    // nav
    private fun Navigation() {
        val buttoncart = findViewById<ImageButton>(R.id.cart)
        val buttonProduk = findViewById<CardView>(R.id.produkcard)
        val buttonMarket = findViewById<ImageButton>(R.id.btnmarket)
        val buttonHistory = findViewById<ImageButton>(R.id.btnhistory)
        val buttonProfile = findViewById<ImageButton>(R.id.btnprofil)
        val buttonEgg = findViewById<LinearLayout>(R.id.egg)
        val buttonPoultry = findViewById<LinearLayout>(R.id.poultry)
        val buttonMeat = findViewById<LinearLayout>(R.id.meat)
        val buttonSeed = findViewById<LinearLayout>(R.id.seed)

        buttoncart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        buttonProduk.setOnClickListener {
            startActivity(Intent(this, ProdukActivity::class.java))
        }

        buttonHistory.setOnClickListener {
            startActivity(Intent(this, CartCompleteActivity::class.java))
        }

        buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }

        buttonMarket.setOnClickListener {
            startActivity(Intent(this, LocationStoreActivity::class.java))
        }

        buttonEgg.setOnClickListener {
            val categoryIds = "5,6"
            val intent = Intent(this@DashboardActivity, EggCategoryActivity::class.java).apply {
                putExtra("categoryIds", categoryIds)
            }
            startActivity(intent)
        }

        buttonMeat.setOnClickListener {
            val categoryIds = "3,4"
            val intent = Intent(this@DashboardActivity, MeatCategoryActivity::class.java).apply {
                putExtra("categoryIds", categoryIds)
            }
            startActivity(intent)
        }

        buttonPoultry.setOnClickListener {
            val categoryIds = "1,2"
            val intent = Intent(this@DashboardActivity, PoultryCategoryActivity::class.java).apply {
                putExtra("categoryIds", categoryIds)
            }
            startActivity(intent)
        }

        buttonSeed.setOnClickListener {
            val categoryIds = "7"
            val intent = Intent(this@DashboardActivity, SeedCategoryActivity::class.java).apply {
                putExtra("categoryIds", categoryIds)
            }
            startActivity(intent)
        }

    }

    // func to get username by email to greet them
    private suspend fun updateUserGreeting(greetUser: TextView) {
        val auth = supabase.auth
        val userEmail = auth.retrieveUserForCurrentSession(updateSession = true).email

        if (userEmail != null) {
            val requestBody = mapOf("p_email" to userEmail)
            val response: Response<String> = RetrofitClient.instance.getUserByEmail(requestBody)

            if (response.isSuccessful) {
                val displayName = response.body()?.removeSurrounding("\"")
                greetUser.text = "Welcome, ${displayName ?: "User"}"
            } else {
                greetUser.text = "Welcome, User"
            }
        } else {
            greetUser.text = "Welcome, Guest"
        }
    }

    // get user's id by email to get their avatar's path
    private suspend fun getUserIdByEmail(email: String): Int? {
        val requestBody = mapOf("user_email" to email)
        val response: Response<Int> = RetrofitClient.instance.getUserIdByEmail(requestBody)

        if (response.isSuccessful) {
            Log.d("APIResponse", "User ID retrieved: ${response.body()}")
            return response.body()
        } else {
            Log.e("APIError", "Failed to retrieve user ID: ${response.errorBody()?.string()}")
            return null
        }
    }

    // load user's avatar from supabase
    private fun loadImageFromSupabase(filePath: String) {
        lifecycleScope.launch {
            try {
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath?t=${System.currentTimeMillis()}"

                Glide.with(this@DashboardActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.fotoprofil)
                    .error(R.drawable.fotoprofil)
                    .into(findViewById<CircleImageView>(R.id.user_pfp))
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }


    // load products from table produk
    private fun loadProducts() {
        lifecycleScope.launch {
            try {
                val response = supabase.postgrest["produk"].select()
                if (response.data is String) {
                    Log.d("ProductLoad", "productformat: ${response.data}")
                    val productsJson = response.data
                    val gson = Gson()
                    val productType = object : TypeToken<Array<Products>>() {}.type

                    val products: Array<Products> = gson.fromJson(productsJson, productType)
                    Log.d("ProductLoad", "products: ${products.toList()}")

                    allProducts = products.toList()

                    displayProducts(allProducts)
                } else {
                    Log.e("ProductLoadError", "Unexpected response format: ${response.data}")
                }
            } catch (e: Exception) {
                Log.e("ProductLoadError", "Failed to load products: ${e.message}")
            }
        }
    }

    // price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
    }

    // display loaded products into grid
    private fun displayProducts(products: List<Products>) {
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
            if (index % 2 == 0) {
                params.setMargins(30, 20, 10, 20)
            } else {
                params.setMargins(20, 20, 10, 20)
            }

            cardView.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val ratingData = fetchProductRating(product.id)
                        val averageRating = ratingData?.first ?: 0.0
                        val totalReviews = ratingData?.second ?: 0

                        val intent = Intent(this@DashboardActivity, ProdukActivity::class.java).apply {
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

    // get each products avg rating and total review
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

    // filter products by search input
    private fun filterProducts(query: String) {
        val filteredProducts = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.nama_produk.contains(query, ignoreCase = true) }
        }
        displayProducts(filteredProducts)
    }
}