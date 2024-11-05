package com.example.splashscreen

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.yourapp.network.RetrofitClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.launch

class AllCommentActivity : AppCompatActivity() {

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
        enableEdgeToEdge()
        setContentView(R.layout.all_comment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val productId = intent.getLongExtra("product_id", 0)
        lifecycleScope.launch {
            fetchProductReviews(productId)
        }
    }

    // get all reviews of that product
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

    // display the reviews into linear layout
    private fun displayReviews(reviews: List<Map<String, Any>>) {
        val reviewContainer = findViewById<LinearLayout>(R.id.review_container)
        reviewContainer.removeAllViews()

        for (review in reviews) {
            val reviewView = layoutInflater.inflate(R.layout.review, reviewContainer, false)

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
}