package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.homepage.HomeActivity
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.NumberFormat
import java.util.Locale


class DashboardActivity : AppCompatActivity() {

    private lateinit var allProducts: List<ProductData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        // Retrieve the TOKEN passed from the previous activity
//        val token = "Bearer " + intent.getStringExtra("TOKEN")
        val storedtoken = "Bearer ${getStoredToken().toString()}"
        Log.d("storedtoken", "$storedtoken")

        Navigation()

        val greetUser = findViewById<TextView>(R.id.greet)
        val userLocation = findViewById<TextView>(R.id.user_location)
        val userpfp = findViewById<CircleImageView>(R.id.user_pfp)
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)

        // Initialize the search input
        val searchInput = findViewById<EditText>(R.id.searchInput)

        // Set up TextWatcher for real-time filtering
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
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
                    putExtra("TOKEN", storedtoken)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // Make the API call to get the user profile
        getProfile(storedtoken, greetUser, userLocation, userpfp)
        getProducts(storedtoken, gridLayout)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProfilePicUpdate(event: Event.ProfilePicUpdateEvent) {
        // Get reference to the profile image view
        val userPfp = findViewById<CircleImageView>(R.id.user_pfp)

        // Get the token and make API call to refresh profile
        val token = "Bearer ${getStoredToken().toString()}"

        // Refresh profile data including the image
        RetrofitClient.instance.getProfile(token)
            .enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {
                        val buyerData = response.body()?.data
                        val userId = buyerData?.id ?: 0
                        // Force refresh the profile picture
//                        updateImageFromSupabase("$userId/1.jpg", userPfp, forceRefresh = true)
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                    Log.e("ProfileUpdate", "Failed to update profile: ${t.message}")
                }
            })
    }

    override fun onResume() {
        super.onResume()

        val greetUser = findViewById<TextView>(R.id.greet)
        val userPfp = findViewById<CircleImageView>(R.id.user_pfp)
        val userLocation = findViewById<TextView>(R.id.user_location)


        // Retrieve the token and update profile
        val token = "Bearer ${getStoredToken().toString()}"
        getProfile(token, greetUser, userLocation, userPfp)
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

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

    // load user's avatar to show in their review
    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView) {
        val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath/1.jpg"

        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .override(100, 100)
            .placeholder(R.drawable.fotoprofil)
            .error(R.drawable.fotoprofil)
            .into(imageView)
    }

    private fun getProfile(token: String?, greetUser: TextView, userLocation: TextView, userPfp: CircleImageView) {
        RetrofitClient.instance.getProfile(token ?: "")
            .enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {
                        val buyerData = response.body()?.data
                        val username = buyerData?.user?.username ?: "User"
                        val userkota = buyerData?.kota
                        val usernegara = buyerData?.negara
                        val userId = buyerData?.id ?: 0
                        greetUser.text = "Hello, $username!"
                        loadImageFromSupabase("$userId/1.jpg")
                        Log.d("getprofileresponse", "${response.body()}")
                        if (userkota.isNullOrEmpty() || usernegara.isNullOrEmpty()){
                            userLocation.text = "Somewhere"
                        } else{
                            userLocation.text = "$userkota, $usernegara"
                        }
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }

    // load user's avatar from supabase
    private fun loadImageFromSupabase(filePath: String) {
        lifecycleScope.launch {
            try {
                // Construct the public URL to the object in the storage bucket
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath?t=${System.currentTimeMillis()}"

                // Use Glide to load the image into the ImageView
                Glide.with(this@DashboardActivity)
                    .load(imageUrl)
                    .override(100, 100)
                    .placeholder(R.drawable.fotoprofil)
//                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(findViewById<CircleImageView>(R.id.user_pfp))
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }

    private fun getProducts(token: String, gridLayout: GridLayout) {
        Log.d("gettingproducts", "getting products")
        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        val products = response.body()?.data ?: emptyList()
                        Log.d("getproducts", "$products")
                        getReviews(token, products, gridLayout)
                    } else {
                        // Handle different error cases based on the response code
                        when (response.code()) {
                            401 -> Log.e("getproducts", "Unauthorized access. Check token validity.")
                            403 -> Log.e("getproducts", "Access forbidden.")
                            404 -> Log.e("getproducts", "Products not found.")
                            else -> Log.e("getproducts", "Server error: ${response.code()} ${response.message()}")
                        }
                        // Display an error message on the UI
                        displayErrorMessage(gridLayout, "Failed to load products. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    Log.e("getproducts", "Network failure: ${t.localizedMessage}", t)
                    // Display a network error message on the UI
                    displayErrorMessage(gridLayout, "Network error. Check your connection and try again.")
                }
            })
    }

    // Function to display an error message in the GridLayout
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
        } catch (e: Exception) {
            Log.e("ImageLoadError", "Failed to load product image: ${e.message}")
        }
    }

    private fun displayProducts(products: List<ProductData>, reviews: List<ReviewData>, gridLayout: GridLayout) {
        val sortedProducts = products.sortedByDescending { it.id }
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
                val intent = Intent(this@DashboardActivity, ProdukActivity::class.java).apply {
                    putExtra("product_id", product.id)
                    putExtra("productName", product.nama_produk)
                    putExtra("productImage", product.image)
                    putExtra("productRating", "%.1f".format(averageRating).toFloat())
                    putExtra("productTotalReviews", totalReviews)
                    putExtra("productPrice", product.harga.toLong())
                    putExtra("productDesc", product.deskripsi)
                    putExtra("supplierId", product.supplier_id)
                    product.supplier?.buyer?.let { it1 -> putExtra("supplierImage", it1.id) }
                    putExtra("supplierKota", product.supplier?.kota)
                    putExtra("supplierNegara", product.supplier?.negara)
                    putExtra("supplierProvinsi", product.supplier?.provinsi)
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

    // price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
    }
}