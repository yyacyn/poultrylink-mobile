package com.example.splashscreen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
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
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.net.URL

class AllCommentActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.all_comment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.back_btn).setOnClickListener {
            finish()
        }


        val token = "Bearer ${getStoredToken()}"

        val productId = intent.getLongExtra("product_id", 0)

        getProducts(token, productId.toString())
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun getReviews(token: String, products: List<ProductData>, currentProductId: String) {
        RetrofitClient.instance.getReviews(token)
            .enqueue(object : Callback<ReviewResponse> {
                override fun onResponse(call: Call<ReviewResponse>, response: Response<ReviewResponse>) {
                    if (response.isSuccessful) {
                        val reviews = response.body()?.data ?: emptyList()
                        displayReviews(reviews,currentProductId)
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ReviewResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
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

        for (review in filteredReviews) {
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
                loadImageFromSupabase(avatar_path, avatarImageView)
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

    // load user's avatar from supabase
    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView) {
        lifecycleScope.launch {
            try {
                // Construct the public URL to the object in the storage bucket
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath/1.jpg?t=${System.currentTimeMillis()}"

                // Use Glide to load the image into the ImageView
                Glide.with(this@AllCommentActivity)
                    .load(imageUrl)
                    .override(50, 50)
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(imageView)
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }


    private fun getProducts(token: String, product_id: String) {
        RetrofitClient.instance.getProducts(token)
            .enqueue(object : Callback<ProductResponse> {
                override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                    if (response.isSuccessful) {
                        val products = response.body()?.data ?: emptyList()
                        getReviews(token, products, product_id)
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }
}