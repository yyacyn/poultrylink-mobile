package com.example.splashscreen

import ApiService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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
        val productImage = intent.getStringExtra("productImage")
        val productRating = intent.getFloatExtra("productRating", 0.0F)
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productDesc = intent.getStringExtra("productDesc")
        val productSupplierId = intent.getLongExtra("supplierId", 0)

        // Set the data to views
        findViewById<TextView>(R.id.product_name).text = productName
        val value = formatWithDots(productPrice)
        findViewById<TextView>(R.id.product_price).text = "Rp. $value"
        findViewById<TextView>(R.id.product_rating).text = productRating.toString()
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
}
