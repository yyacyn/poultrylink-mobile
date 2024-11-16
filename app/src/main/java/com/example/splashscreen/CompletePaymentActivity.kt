package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class CompletePaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.complete_payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val orderId = intent.getStringExtra("orderId")

        findViewById<TextView>(R.id.textViewOrderNumber).text = " #$orderId"
        findViewById<MaterialButton>(R.id.continueBtn).setOnClickListener {
            val intent = Intent(this@CompletePaymentActivity, DashboardActivity::class.java)
            startActivity(intent)
        }
    }
}