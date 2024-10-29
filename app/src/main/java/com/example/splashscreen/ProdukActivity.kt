package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel


class ProdukActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.produk)

        val imageSlider = findViewById<ImageSlider>(R.id.imageSlider)
        val slideModels = ArrayList<SlideModel>()
        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        // Menambahkan gambar ke dalam slide model
        slideModels.add(SlideModel(R.drawable.ayam, ScaleTypes.FIT))
        slideModels.add(SlideModel(R.drawable.ayam, ScaleTypes.FIT))
        slideModels.add(SlideModel(R.drawable.ayam, ScaleTypes.FIT))

        // Mengatur daftar gambar pada imageSlider
        imageSlider.setImageList(slideModels, ScaleTypes.FIT)

        btnBack.setOnClickListener{
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
