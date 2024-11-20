package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConfirmPaymentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirm_payment)

        val orderId = intent.getStringExtra("orderId")
        val orderInvoice = intent.getStringExtra("orderInvoice")

        val timerTextView = findViewById<TextView>(R.id.textViewTimer)
        findViewById<TextView>(R.id.textViewOrderNumber).text = "$orderInvoice"

        val token = "Bearer ${getStoredToken()}"

        val backButton = findViewById<ImageButton>(R.id.backBtn)

        // Start a countdown timer
        val countDownTimer = object : CountDownTimer(7 * 60 * 1000, 1000) { // 7 minutes
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                // Update the timer text every second
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)

                // Check if the timer reaches 6:55
                if (minutes == 6L && seconds == 55L) {
                    cancel() // Stop the timer

                    if (orderId != null) {
                        confirmOrder(token, orderId.toInt())
                    }

                    // Navigate to CompletePaymentActivity
                    val intent = Intent(this@ConfirmPaymentActivity, CompletePaymentActivity::class.java).apply {
                        putExtra("orderId", orderId)
                        putExtra("orderInvoice", orderInvoice)
                    }
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFinish() {
                // Handle if timer finishes naturally
                timerTextView.text = "00:00"
            }
        }

        // Start the timer
        countDownTimer.start()

        // Handle back button click
        backButton.setOnClickListener {
            // Stop the timer and navigate back to DashboardActivity
            countDownTimer.cancel()
            val intent = Intent(this@ConfirmPaymentActivity, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun confirmOrder(token: String, orderId: Int) {
        val orderRequest = RetrieveOrderRequest(orderId)
        RetrofitClient.instance.confirmOrder(token, orderRequest).enqueue(object :
            Callback<CancelOrderResponse> {
            override fun onResponse(call: Call<CancelOrderResponse>, response: Response<CancelOrderResponse>) {
                if (response.isSuccessful) {
                    response.body()?.order?.let { order ->
                        // Navigate to ConfirmPaymentActivity with order details
                    } ?: run {
                        Log.e("OrderError", "Null order in response.")
                    }
                } else {
                    Log.e("OrderError", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<CancelOrderResponse>, t: Throwable) {
                Log.e("OrderError", "Failed to create order: ${t.message}")
            }
        })
    }
}
