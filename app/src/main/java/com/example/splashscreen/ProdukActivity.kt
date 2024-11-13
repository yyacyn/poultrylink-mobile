package com.example.splashscreen

import ApiService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.homepage.HomeActivity
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.NumberFormat
import java.util.Locale


class ProdukActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.produk)

        // retrieve passed product data
        val productName = intent.getStringExtra("productName")
        val productId = intent.getLongExtra("product_id", 0)
        val productImage = intent.getStringExtra("productImage")
        val productRating = intent.getFloatExtra("productRating", 0.0F)
        val productTotalReviews = intent.getIntExtra("productTotalReviews", 0)
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productDesc = intent.getStringExtra("productDesc")
        val productSupplierId = intent.getLongExtra("supplierId", 0)
        val supplierKota = intent.getStringExtra("supplierKota")
        val supplierNegara = intent.getStringExtra("supplierNegara")
        val supplierToko = intent.getStringExtra("supplierToko")
        val productCategory = intent.getStringExtra("productCategory")
        val product_id = intent.getLongExtra("product_id", 0)
        val location = intent.getStringExtra("location")
        Log.d("product_id", "$product_id")

        Navigation(productId,location.toString())

        val token = "Bearer ${getStoredToken().toString()}"


        findViewById<TextView>(R.id.seller_name).text = supplierToko
        findViewById<TextView>(R.id.seller_location).text = "$supplierKota, $supplierNegara"
        findViewById<TextView>(R.id.product_name).text = productName
        val value = formatWithDots(productPrice)
        findViewById<TextView>(R.id.product_price).text = "Rp. $value"
        findViewById<TextView>(R.id.product_rating).text = productRating.toString()
        findViewById<TextView>(R.id.product_desc).text = productDesc

        // set up ImageSlider for multiple images
        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val slideModels = arrayListOf(
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/1.jpg", ScaleTypes.FIT),
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/2.jpg", ScaleTypes.FIT),
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/3.jpg", ScaleTypes.FIT)
        )
        imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP)

        val btnCard = findViewById<ImageButton>(R.id.cart)
        if (productCategory != null) {
            getProducts(token, findViewById<GridLayout>(R.id.gridLayout), productCategory, product_id.toInt())
        }


    }

    // nav
    private fun Navigation(product_id: Long, location: String) {

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.lihatsemua).setOnClickListener {
            val intent = Intent(this@ProdukActivity, AllCommentActivity::class.java).apply {
                putExtra("product_id", product_id)
            }
            startActivity(intent)

        }
    }

    private fun displayReviews(reviews: List<ReviewData>, currentProductId: String) {
        val reviewContainer = findViewById<LinearLayout>(R.id.review_container)
        reviewContainer.removeAllViews() // Clear previous reviews

        Log.d("reviews", "$reviews")

        // Add logging to check the currentProductId
        Log.d("currentProductId", "Current ID: $currentProductId")

        // Filter reviews to only include reviews for the current product based on product_id
        val filteredReviews = reviews.filter { review ->
            Log.d("comparison", "Comparing ${review.produk_id} with $currentProductId")
            review.produk_id == currentProductId
        }

        // Display only the first 2 filtered reviews (can be adjusted to show more if necessary)
        for (review in filteredReviews.take(2)) {
            val reviewView = layoutInflater.inflate(R.layout.review_card, reviewContainer, false)

            // Extract fields from the review data
            val username = review.user.username
            val ulasan = review.ulasan
            val rating = review.rating
            val avatar_path = review.buyer.avatar_path

            reviewView.findViewById<TextView>(R.id.username).text = username
            reviewView.findViewById<TextView>(R.id.ulasan).text = ulasan

            if (!avatar_path.isNullOrEmpty()) {
                val avatarImageView = reviewView.findViewById<CircleImageView>(R.id.user_pfp)
                loadUserAvatar(avatar_path, avatarImageView)
            }

            val ratingLayout = reviewView.findViewById<LinearLayout>(R.id.rating_layout)
            ratingLayout.removeAllViews()

            for (i in 1..5) {
                val star = ImageView(this)
                star.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                star.setImageResource(R.drawable.star)

                if (i <= rating) {
                    ratingLayout.addView(star)
                }
            }
            reviewContainer.addView(reviewView)
        }
    }


    // price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount) // This will automatically add dots without "Rp" symbol
    }


    private fun displayRecommendedProducts(products: List<ProductData>, reviews: List<ReviewData>, gridLayout: GridLayout) {
        gridLayout.removeAllViews()
        for ((index, product) in products.withIndex().shuffled().take(4)) {
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
            Log.d("productreviews", "$productReviews")
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
                val intent = Intent(this@ProdukActivity, ProdukActivity::class.java).apply {
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


    private fun loadUserAvatar(filePath: String, imageView: CircleImageView, forceRefresh: Boolean = false) {
        lifecycleScope.launch {
            try {
                val baseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath/1.jpg"
                val imageUrl = if (forceRefresh) {
                    "$baseUrl?t=${System.currentTimeMillis()}"
                } else {
                    baseUrl
                }

                // Directly load image from URL as Bitmap
                withContext(Dispatchers.IO) {
                    val urlConnection = URL(imageUrl).openConnection()
                    val originalBitmap = BitmapFactory.decodeStream(urlConnection.getInputStream())

                    // Resize the image
                    val desiredWidth = 100 // Set the desired width
                    val desiredHeight = 100 // Set the desired height
                    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, true)

                    // Compress the image
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Set the quality (0-100)
                    val compressedByteArray = outputStream.toByteArray()

                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.size))
                    }
                }

                Log.d("imageloaded", "image loaded from: $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }


    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun getRecommendedCategoryRange(categoryId: String): List<String> {
        return when (categoryId) {
            "1", "2" -> listOf("1", "2")
            "3", "4" -> listOf("3", "4")
            "5", "6" -> listOf("5", "6")
            "7" -> listOf("7")
            else -> emptyList() // For unexpected categories
        }
    }

    private fun getProducts(token: String, gridLayout: GridLayout, currentCategoryId: String, currentProductId: Int) {
        val categoryRange = getRecommendedCategoryRange(currentCategoryId)

        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        val products = response.body()?.data?.filter {
                            it.kategori_id in categoryRange && it.id.toInt() != currentProductId
                        } ?: emptyList()

                        Log.d("filteredproducts", products.toString())

                        getReviews(token, products, gridLayout, currentProductId.toString())
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }

    private fun getReviews(token: String, products: List<ProductData>, gridLayout: GridLayout, currentProductId: String) {
        RetrofitClient.instance.getReviews(token)
            .enqueue(object : Callback<ReviewResponse> {
                override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
                    if (response.isSuccessful) {
                        val reviews = response.body()?.data ?: emptyList()
                        displayReviews(reviews,currentProductId)
                        displayRecommendedProducts(products, reviews, gridLayout)
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }
}
