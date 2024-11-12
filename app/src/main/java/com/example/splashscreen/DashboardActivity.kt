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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.homepage.HomeActivity
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var allProducts: List<ProductData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        // Retrieve the TOKEN passed from the previous activity
        val token = "Bearer " + intent.getStringExtra("TOKEN")

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
                    putExtra("TOKEN", token)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // Make the API call to get the user profile
        getProfile(token, greetUser, userLocation, userpfp)
        getProducts(token, gridLayout)
    }

//    private fun filterProducts(query: String, gridLayout: GridLayout, token: String) {
//        val filteredProducts = if (query.isEmpty()) {
//            allProducts
//        } else {
//            allProducts.filter { it.nama_produk.contains(query, ignoreCase = true) }
//        }
//        displayProducts(filteredProducts, gridLayout, token)
//    }

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
                        loadImageFromSupabase("$userId/1.jpg", userPfp)
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

    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView) {
        lifecycleScope.launch {
            try {
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath?t=${System.currentTimeMillis()}"

                Glide.with(this@DashboardActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.fotoprofil)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(true)
                    .override(100, 100)
                    .error(R.drawable.fotoprofil)
                    .into(imageView)
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }

    private fun getProducts(token: String, gridLayout: GridLayout) {
        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        val products = response.body()?.data ?: emptyList()
                        getReviews(token, products, gridLayout)
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
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

    private fun displayProducts(products: List<ProductData>, reviews: List<ReviewData>, gridLayout: GridLayout) {
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
                    putExtra("productRating", averageRating.toFloat())
                    putExtra("productTotalReviews", totalReviews)
                    putExtra("productPrice", product.harga)
                    putExtra("productDesc", product.deskripsi)
                    putExtra("supplierId", product.supplier_id)
                }
                startActivity(intent)
            }

            cardView.layoutParams = params
            gridLayout.addView(cardView)
        }
    }

//    private fun getReviews(productId: String, token: String, callback: (Double, Int) -> Unit) {
//        RetrofitClient.instance.getReviews(token)
//            .enqueue(object : Callback<ReviewResponse> {
//                override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
//                    if (response.isSuccessful) {
//                        val reviews = response.body()?.data ?: emptyList()
//                        Log.d("reviews", "$reviews")
//                        val productReviews = reviews.filter { it.produk_id == productId }
//
//                        val averageRating = if (productReviews.isNotEmpty()) {
//                            productReviews.mapNotNull { it.rating }.average()
//                        } else {
//                            0.0
//                        }
//                        val totalReviews = productReviews.size
//
//                        // Invoke callback with the calculated values
//                        callback(averageRating, totalReviews)
//                    } else {
//                        // Handle unsuccessful response
//                        callback(0.0, 0)
//                    }
//                }
//
//                override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
//                    // Handle failure and call the callback with default values
//                    Log.e("getReviews", "Error: ${t.message}")
//                    callback(0.0, 0)
//                }
//            })
//    }


    // price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount)
    }
}