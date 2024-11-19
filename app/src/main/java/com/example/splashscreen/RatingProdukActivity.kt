package com.example.splashscreen

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RatingProdukActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.rating_produk)

        val rbRatingBar = findViewById<RatingBar>(R.id.ratingproduk)
        val reviewInput = findViewById<EditText>(R.id.reviewInput)
        val submitButton = findViewById<MaterialButton>(R.id.submitButton)

        val token = "Bearer ${getStoredToken()}"
        val productId = intent.getLongExtra("productId", 0)
        val productImage = intent.getStringExtra("productImage")
        val productName = intent.getStringExtra("productName")
        val productCategory = intent.getStringExtra("productCategory")

        val orangeColor = ContextCompat.getColor(this, R.color.orange)
        rbRatingBar.rating = 0f
        rbRatingBar.stepSize = 1F
        rbRatingBar.backgroundTintList = ColorStateList.valueOf(orangeColor)

        findViewById<TextView>(R.id.productName).text = productName
        findViewById<TextView>(R.id.productCategory).text = productCategory

        val supplierImageView = findViewById<ImageView>(R.id.productImage)
        loadImageFromSupabase(productImage.toString(), supplierImageView)

        rbRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            Toast.makeText(this, "Rating: $rating", Toast.LENGTH_SHORT).show()
        }

        submitButton.setOnClickListener {
            val ulasan = reviewInput.text.toString().trim()
            val rating = rbRatingBar.rating.toInt()

            if (ulasan.isEmpty() || rating == 0) {
                Toast.makeText(this, "Please enter a review and a rating!", Toast.LENGTH_SHORT).show()
            } else {
                postReview(token, productId, ulasan, rating)
            }
        }
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun loadImageFromSupabase(imagePath: String, imageView: ImageView, forceRefresh: Boolean = false) {
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

    private fun postReview(token: String, productId: Long, ulasan: String, rating: Int) {

        val reviewRequest = ReviewPostRequest(productId, ulasan, rating)

        Log.d("reviewrequest", "$reviewRequest")

        val request = RetrofitClient.instance.postReview(token, reviewRequest)

        // Add Authorization header with the token
        request.enqueue(object : Callback<PostReviewResponse> {
            override fun onResponse(call: Call<PostReviewResponse>, response: Response<PostReviewResponse>) {
                if (response.isSuccessful) {
                    val response = response.body()
                    Log.d("reviewResponse", "$response")
                    if (response != null) {
                        if (response.success) {
                            Toast.makeText(this@RatingProdukActivity, "Review submitted~", Toast.LENGTH_SHORT).show()
                            Log.d("review", "Review submitted successfully: $response")
                            val intent = Intent(this@RatingProdukActivity, DashboardActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@RatingProdukActivity, "Review not submitted~: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() // Debug server errors
                    Toast.makeText(this@RatingProdukActivity, "Review not submitted~: ${response.message()}", Toast.LENGTH_SHORT).show()
                    Log.e("review", "Failed to submit review: ${response.message()}, Error: $errorBody")
                }
            }

            override fun onFailure(call: Call<PostReviewResponse>, t: Throwable) {
                Log.e("review", "Failed to submit review: ${t.message}")
            }
        })
    }

}