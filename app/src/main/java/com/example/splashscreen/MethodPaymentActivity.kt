package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MethodPaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.method_payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buyNow = intent.getBooleanExtra("buyNow", false)

        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productPrice = intent.getLongExtra("productPrice", 0)
        val productQty = intent.getIntExtra("productQty", 0)
        val productId = intent.getLongExtra("productId",0)
        val cartId = intent.getLongExtra("cartId",0)

        if (!buyNow) {
            findViewById<CardView>(R.id.transfer).setOnClickListener{
                val intent = Intent(this@MethodPaymentActivity, PaymentActivity::class.java).apply {
                    putExtra("method", "transfer")
                    putExtra("image", "visa")
                }
                startActivity(intent)
            }

            findViewById<CardView>(R.id.cash).setOnClickListener{
                val intent = Intent(this@MethodPaymentActivity, PaymentActivity::class.java).apply {
                    putExtra("method", "cash")
                    putExtra("image", "cash")
                }
                startActivity(intent)
            }
            findViewById<CardView>(R.id.indomaretPayment).setOnClickListener{
                val intent = Intent(this@MethodPaymentActivity, PaymentActivity::class.java).apply {
                    putExtra("method", "indomaret")
                    putExtra("image", "indomaret")
                }
                startActivity(intent)
            }

            findViewById<ImageButton>(R.id.backbtn).setOnClickListener {
                finish()
            }
        } else {
            findViewById<CardView>(R.id.transfer).setOnClickListener{
                val intent = Intent(this@MethodPaymentActivity, BuyNowActivity::class.java).apply {
                    putExtra("method", "transfer")
                    putExtra("image", "visa")
                    putExtra("productName", productName)
                    putExtra("productImage", productImage)
                    putExtra("productPrice", productPrice)
                    putExtra("productQty", productQty)
                    putExtra("productId", productId)
                    putExtra("cartId", cartId)
                }
                startActivity(intent)
            }

            findViewById<CardView>(R.id.cash).setOnClickListener{
                val intent = Intent(this@MethodPaymentActivity, BuyNowActivity::class.java).apply {
                    putExtra("method", "cash")
                    putExtra("image", "cash")
                    putExtra("productName", productName)
                    putExtra("productImage", productImage)
                    putExtra("productPrice", productPrice)
                    putExtra("productQty", productQty)
                    putExtra("productId", productId)
                    putExtra("cartId", cartId)
                }
                startActivity(intent)
            }
            findViewById<CardView>(R.id.indomaretPayment).setOnClickListener{
                val intent = Intent(this@MethodPaymentActivity, BuyNowActivity::class.java).apply {
                    putExtra("method", "indomaret")
                    putExtra("image", "indomaret")
                    putExtra("productName", productName)
                    putExtra("productImage", productImage)
                    putExtra("productPrice", productPrice)
                    putExtra("productQty", productQty)
                    putExtra("productId", productId)
                    putExtra("cartId", cartId)
                }
                startActivity(intent)
            }

            findViewById<ImageButton>(R.id.backbtn).setOnClickListener {
                finish()
            }
        }

    }
}