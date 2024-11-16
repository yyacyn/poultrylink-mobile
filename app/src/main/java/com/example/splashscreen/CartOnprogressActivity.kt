package com.example.splashscreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yourapp.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CartOnprogressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cart_onprogress)
        val mycart = findViewById<TextView>(R.id.mycart)
        val onprocess = findViewById<TextView>(R.id.onprocess)
        val complete = findViewById<TextView>(R.id.complete)

        val token = "Bearer ${getStoredToken()}"

        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                fetchOrderDetail(token, userId)
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }

        mycart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        onprocess.setOnClickListener {
            val intent = Intent(this, CartOnprogressActivity::class.java)
            startActivity(intent)
        }

        complete.setOnClickListener {
            val intent = Intent(this, CartCompleteActivity::class.java)
            startActivity(intent)
        }
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    private fun getUser(token: String, callback: (Long?) -> Unit) {
        RetrofitClient.instance.getUser(token)
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        val userId = response.body()?.id
                        Log.d("getUser", "User ID: $userId")
                        if (userId != null) {
                            callback(userId.toLong())
                        } // Return the user ID via callback
                    } else {
                        Log.e("FetchCarts", "Error: ${response.code()}")
                        callback(null) // Return null if there's an error
                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {
                    Log.e("FetchCarts", "Network Error: ${t.message}")
                    callback(null) // Return null if there's a failure
                }
            })
    }

    private fun fetchOrderDetail(token: String, userId: Long) {
        RetrofitClient.instance.getOrderDetail(token)
            .enqueue(object : Callback<OrderDetailResponse> {
                override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                    if (response.isSuccessful) {
                        val orderdetail = response.body()?.data
                        Log.d("FetchOrderDetail", "Order Details: $orderdetail")
                    } else {
                        Log.e("FetchOrderDetail", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                    Log.e("FetchOrderDetail", "Network Error: ${t.message}")
                }
            })
    }
}