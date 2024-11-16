package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
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

        findViewById<CardView>(R.id.backbtn).setOnClickListener {
            finish()
        }

    }
}