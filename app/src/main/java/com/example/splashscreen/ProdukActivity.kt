package com.example.splashscreen

import ApiService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
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
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale


class ProdukActivity : AppCompatActivity() {
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.produk)

        // Retrieve passed product data
        val productName = intent.getStringExtra("productName")
        val productId = intent.getLongExtra("product_id", 0)
        val productImage = intent.getStringExtra("productImage")
        val productRating = intent.getFloatExtra("productRating", 0.0F)
        val productTotalReviews = intent.getIntExtra("productTotalReviews", 0)
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productDesc = intent.getStringExtra("productDesc")
        val productSupplierId = intent.getLongExtra("supplierId", 0)

        // Set the data to views
        findViewById<TextView>(R.id.product_name).text = productName
        val value = formatWithDots(productPrice)
        findViewById<TextView>(R.id.product_price).text = "Rp. $value"
        findViewById<TextView>(R.id.product_rating).text = productRating.toString()
//        findViewById<TextView>(R.id.product_total_reviews).text = "($productTotalReviews Reviews)"
        findViewById<TextView>(R.id.product_desc).text = productDesc

        // Set up ImageSlider for multiple images
        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val slideModels = arrayListOf(
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/1.jpg", ScaleTypes.FIT),
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/2.jpg", ScaleTypes.FIT),
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/3.jpg", ScaleTypes.FIT)
        )
        imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP)

        // Fetch and display supplier name
        lifecycleScope.launch {
            fetchSupplierName(productSupplierId, findViewById(R.id.seller_name), findViewById(R.id.seller_location))
        }

        // Fetch and display product reviews
        lifecycleScope.launch {
            fetchProductReviews(productId)
        }

        // Fetch and display products with the same category
        lifecycleScope.launch {
            fetchSameCategoryProducts(productId)
        }

        // Back button functionality
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount) // This will automatically add dots without "Rp" symbol
    }

    private suspend fun fetchSupplierName(supplierId: Long, supplierNameTextView: TextView, supplierLocationTextView: TextView) {
        // Log the supplierId being sent
        Log.d("suppliercheck", "Fetching supplier for ID: $supplierId")

        // Create a request map for the supplier ID
        val requestBody = mapOf("supplier_id" to supplierId)
        Log.d("suppliercheck", "Request body: $requestBody")

        // Make the API call using the ApiService from RetrofitClient
        val response: Response<List<Supplier>> = RetrofitClient.instance.getSupplierById(requestBody)

        // Check if the response is successful
        if (response.isSuccessful) {
            val supplierData = response.body()
            Log.d("suppliercheck", "Response data: $supplierData")
            if (!supplierData.isNullOrEmpty()) {
                val supplierName = supplierData[0].nama_toko
                val supplierCity = supplierData[0].kota
                val supplierCountry = supplierData[0].negara// Access the supplier name

                val supplierLocation = "$supplierCity, $supplierCountry"

                supplierNameTextView.text = supplierName
                supplierLocationTextView.text = supplierLocation
            } else {
                supplierNameTextView.text = "Unknown Supplier"
            }
        } else {
            Log.d("suppliercheck", "Response error: ${response.code()} ${response.message()}")
            supplierNameTextView.text = "Error fetching supplier: ${response.message()}"
        }
    }

    private suspend fun fetchProductReviews(productId: Long) {
        try {
            val response = RetrofitClient.instance.getProductReviews(productId)
            if (response.isSuccessful && response.body() != null) {
                val reviews = response.body()!!
                displayReviews(reviews)
            } else {
                Log.e("ReviewFetchError", "Error fetching reviews: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ReviewFetchError", "Exception: ${e.message}")
        }
    }

    private fun displayReviews(reviews: List<Map<String, Any>>) {
        val reviewContainer = findViewById<LinearLayout>(R.id.review_container) // LinearLayout to hold review cards
        reviewContainer.removeAllViews() // Clear any existing reviews

        for (review in reviews.take(2)) { // Get only the two most recent reviews
            val reviewView = layoutInflater.inflate(R.layout.review_card, reviewContainer, false)

            // Extract data from the review map
            val username = review["username"] as String
            val ulasan = review["ulasan"] as String
            val rating = (review["rating"] as Number).toInt() // Use integer for rating

            // Set username and review text
            reviewView.findViewById<TextView>(R.id.username).text = username
            reviewView.findViewById<TextView>(R.id.ulasan).text = ulasan

            // Set rating stars
            val ratingLayout = reviewView.findViewById<LinearLayout>(R.id.rating_layout)
            ratingLayout.removeAllViews() // Clear any existing stars

            for (i in 1..5) {
                val star = ImageView(this)
                star.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                star.setImageResource(R.drawable.star) // Use your star drawable

                // Show star based on the user rating
                if (i <= rating) {
                    ratingLayout.addView(star) // Add star to layout if within rating
                }
            }
            // Add the review card to the review container
            reviewContainer.addView(reviewView)
        }
    }


    private suspend fun fetchSameCategoryProducts(productId: Long) {
        val response: Response<List<Products>> = RetrofitClient.instance.getSameCategoryProducts(
            mapOf("product_id" to productId)
        )

        if (response.isSuccessful && response.body() != null) {
            displayProductsInGrid(response.body()!!)
        } else {
            Log.e("SameCategoryProducts", "Error fetching products: ${response.message()}")
        }
    }

    // display recommended products grid
    private fun displayProductsInGrid(products: List<Products>) {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        gridLayout.removeAllViews() // Clear existing views

        // Shuffle the list and take only four products
        val randomProducts = products.shuffled().take(4)

        for ((index, product) in randomProducts.withIndex()) {
            val cardView = layoutInflater.inflate(R.layout.product_card, gridLayout, false)
            val productImage = cardView.findViewById<ImageView>(R.id.productImage)
            val productName = cardView.findViewById<TextView>(R.id.productName)
            val productRating = cardView.findViewById<TextView>(R.id.productRating)
            val productAmountRating = cardView.findViewById<TextView>(R.id.productAmountRating)
            val productPrice = cardView.findViewById<TextView>(R.id.productPrice)
            val productLocation = cardView.findViewById<TextView>(R.id.productLocation)

            // Load the first image for the product
            val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/${product.image}/1.jpg"
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.emiya)
                .error(R.drawable.sekar)
                .into(productImage)

            productName.text = product.nama_produk

            // Fetch product rating and review count asynchronously
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

            // Format the price
            val value = formatWithDots(product.harga)
            productPrice.text = "Rp. $value"

            // Adjust margins for layout spacing
            val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(if (index % 2 == 0) 30 else 20, 20, 10, 20)

            // Set onClickListener to navigate to ProdukActivity with additional data
            cardView.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val ratingData = fetchProductRating(product.id)
                        val averageRating = ratingData?.first ?: 0.0
                        val totalReviews = ratingData?.second ?: 0

                        val intent = Intent(this@ProdukActivity, ProdukActivity::class.java).apply {
                            putExtra("product_id", product.id)
                            putExtra("productName", product.nama_produk)
                            putExtra("productImage", product.image) // Pass the image base name
                            putExtra("productRating", averageRating.toFloat()) // Convert to Float for intent
                            putExtra("productTotalReviews", totalReviews) // Pass the total reviews
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


    suspend fun fetchProductRating(productId: Long): Pair<Double, Int>? {
        return try {
            val response = RetrofitClient.instance.getProductRating(mapOf("product_id" to productId))

            // Log the raw JSON response for debugging
            Log.d("fetchProductRating", "Response body: ${response.body()}")

            if (response.isSuccessful) {
                val data = response.body()

                // Check if data is an array and has at least one element
                if (data != null && data is List<*>) {
                    val firstItem = data.firstOrNull() as? Map<String, Any>
                    if (firstItem != null) {
                        var averageRating = (firstItem["average_rating"] as? Double) ?: 0.0
                        // Format averageRating to two decimal places
                        averageRating = String.format("%.2f", averageRating).toDouble()

                        // Adjust totalReviews to be an Int from Double
                        val totalReviews = (firstItem["total_reviews"] as? Double)?.toInt() ?: 0
                        return Pair(averageRating, totalReviews)
                    }
                }
                // If there's no valid data, return default values
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
}
