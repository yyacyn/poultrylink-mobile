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
import androidx.cardview.widget.CardView
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

        // retrieve passed product data
        val productName = intent.getStringExtra("productName")
        val productId = intent.getLongExtra("product_id", 0)
        val productImage = intent.getStringExtra("productImage")
        val productRating = intent.getFloatExtra("productRating", 0.0F)
        val productTotalReviews = intent.getIntExtra("productTotalReviews", 0)
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productDesc = intent.getStringExtra("productDesc")
        val productSupplierId = intent.getLongExtra("supplierId", 0)

        Navigation(productId)

        findViewById<TextView>(R.id.product_name).text = productName
        val value = formatWithDots(productPrice)
        findViewById<TextView>(R.id.product_price).text = "Rp. $value"
        findViewById<TextView>(R.id.product_rating).text = productRating.toString()
//        findViewById<TextView>(R.id.product_total_reviews).text = "($productTotalReviews Reviews)"
        findViewById<TextView>(R.id.product_desc).text = productDesc

        // set up ImageSlider for multiple images
        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val slideModels = arrayListOf(
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/1.jpg", ScaleTypes.FIT),
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/2.jpg", ScaleTypes.FIT),
            SlideModel("https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/products/$productImage/3.jpg", ScaleTypes.FIT)
        )
        imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP)

        // get and display supplier name, get products with the same category, get product reviews
        lifecycleScope.launch {
            fetchSupplierName(productSupplierId, findViewById(R.id.seller_name), findViewById(R.id.seller_location))
            fetchSameCategoryProducts(productId)
            fetchProductReviews(productId)
        }
    }

    // nav
    private fun Navigation(product_id: Long) {

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

    // price formatting
    private fun formatWithDots(amount: Long): String {
        val format = NumberFormat.getNumberInstance(Locale("in", "ID"))
        return format.format(amount) // This will automatically add dots without "Rp" symbol
    }

    // get suppplier name from supabase rest api
    private suspend fun fetchSupplierName(supplierId: Long, supplierNameTextView: TextView, supplierLocationTextView: TextView) {
        Log.d("suppliercheck", "Fetching supplier for ID: $supplierId")

        val requestBody = mapOf("supplier_id" to supplierId)
        Log.d("suppliercheck", "Request body: $requestBody")

        val response: Response<List<Supplier>> = RetrofitClient.instance.getSupplierById(requestBody)

        if (response.isSuccessful) {
            val supplierData = response.body()
            Log.d("suppliercheck", "Response data: $supplierData")
            if (!supplierData.isNullOrEmpty()) {
                val supplierName = supplierData[0].nama_toko
                val supplierCity = supplierData[0].kota
                val supplierCountry = supplierData[0].negara

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

    // get product reviews
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

    //display the two most recent reviews
    private fun displayReviews(reviews: List<Map<String, Any>>) {
        val reviewContainer = findViewById<LinearLayout>(R.id.review_container)
        reviewContainer.removeAllViews()

        for (review in reviews.take(2)) {
            val reviewView = layoutInflater.inflate(R.layout.review_card, reviewContainer, false)

            val username = review["username"] as String
            val ulasan = review["ulasan"] as String
            val rating = (review["rating"] as Number).toInt()
            val avatar_path = review["avatar_path"] as String

            reviewView.findViewById<TextView>(R.id.username).text = username
            reviewView.findViewById<TextView>(R.id.ulasan).text = ulasan

            if (!avatar_path.isNullOrEmpty()) {
                val avatarImageView = reviewView.findViewById<ImageView>(R.id.user_pfp)
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

    // load user's avatar to show in their review
    private fun loadUserAvatar(filePath: String, imageView: ImageView) {
        val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath"

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.fotoprofil)
            .error(R.drawable.fotoprofil)
            .into(imageView)
    }


    // get products with the same category as the current one
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

    // display four recommended products grid randomly
    private fun displayProductsInGrid(products: List<Products>) {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)
        gridLayout.removeAllViews()

        val randomProducts = products.shuffled().take(4)

        for ((index, product) in randomProducts.withIndex()) {
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
}
