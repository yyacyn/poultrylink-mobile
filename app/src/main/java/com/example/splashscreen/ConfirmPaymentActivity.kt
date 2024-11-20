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

        val countDownTimer = object : CountDownTimer(7 * 60 * 1000, 1000) { // 7 minutes
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                timerTextView.text = String.format("%02d:%02d", minutes, seconds)

                if (minutes == 6L && seconds == 55L) {
                    cancel()

                    if (orderId != null) {
                        confirmOrder(token, orderId.toInt())
                    }

                    val intent = Intent(this@ConfirmPaymentActivity, CompletePaymentActivity::class.java).apply {
                        putExtra("orderId", orderId)
                        putExtra("orderInvoice", orderInvoice)
                    }
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFinish() {
                timerTextView.text = "00:00"
            }
        }

        countDownTimer.start()

        backButton.setOnClickListener {
            countDownTimer.cancel()
            val intent = Intent(this@ConfirmPaymentActivity, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

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
