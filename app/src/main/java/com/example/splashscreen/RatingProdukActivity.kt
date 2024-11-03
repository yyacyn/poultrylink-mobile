package com.example.splashscreen

import android.os.Bundle
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RatingProdukActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.rating_produk)

        val rb_ratingbar = findViewById<RatingBar>(R.id.ratingproduk)

        rb_ratingbar.rating = 2.5f
        rb_ratingbar.stepSize = .5f

        rb_ratingbar.setOnRatingBarChangeListener { ratingBar, rating, formUser ->
            Toast.makeText(this, "Rating: $rating", Toast.LENGTH_SHORT).show()
        }
    }

}