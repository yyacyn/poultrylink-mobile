package com.example.splashscreen

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.yourapp.network.RetrofitClient
import kotlinx.coroutines.delay
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class CompletePaymentActivity : AppCompatActivity() {

    private var orderDetails: List<OrderDetailData> = listOf()

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



        val token = "Bearer ${getStoredToken()}"

        getUser(token) { userId ->
            if (userId != null) {
                Log.d("MainActivity", "Fetched User ID: $userId")
                if (orderId != null) {
                    fetchOrderDetail(token, orderId.toLong())
                }
            } else {
                Log.e("MainActivity", "Failed to fetch User ID")
            }
        }

        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val downloadButton = findViewById<ImageButton>(R.id.download)

        closeButton.setOnClickListener {
            val intent = Intent(this@CompletePaymentActivity, DashboardActivity::class.java)
            startActivity(intent)
        }

        downloadButton.setOnClickListener {
            // Hide closeButton when starting the download
            closeButton.visibility = View.GONE
            downloadInvoice()

        }
    }


    private fun downloadInvoice() {
        val invoiceView = findViewById<ConstraintLayout>(R.id.main) // Main layout to capture

        // Step 1: Capture View as Bitmap
        val bitmap = captureViewAsBitmap(invoiceView)

        if (bitmap != null) {
            // Step 2: Generate PDF from Bitmap
            val fileName = "Invoice_${System.currentTimeMillis()}.pdf"
            val filePath = generatePdfFromBitmap(bitmap, fileName)

            // Step 3: Notify User
            if (filePath != null) {
                Toast.makeText(this, "Invoice saved to $filePath", Toast.LENGTH_LONG).show()
                val closeButton = findViewById<ImageButton>(R.id.closeButton)
                closeButton.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Failed to save invoice", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper to capture the view as a bitmap
    private fun captureViewAsBitmap(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Helper to generate a PDF file from a bitmap
    private fun generatePdfFromBitmap(bitmap: Bitmap, fileName: String): String? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        // Save to Downloads directory
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, fileName)

        return try {
            file.outputStream().use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Format the price with dots (e.g., 1000 => 1.000)
    private fun formatWithDots(price: String): String {
        return price.reversed().chunked(3).joinToString(".").reversed()
    }

    private fun fetchOrderDetail(token: String, orderId: Long) {
        RetrofitClient.instance.getOrderDetail(token)
            .enqueue(object : Callback<OrderDetailResponse> {
                override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                    if (response.isSuccessful) {
                        val orderdetail = response.body()?.data
                        val filteredOrderDetail = orderdetail?.filter { it.order_id == orderId }
                        if (filteredOrderDetail != null) {
                            displayOrders(token, filteredOrderDetail)
                            displayOrderInfo(token, filteredOrderDetail)
                            Log.d("FetchOrderDetail", "Filtered Order Detail: $filteredOrderDetail")
                        }
                    } else {
                        Log.e("FetchOrderDetail", "Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                    Log.e("FetchOrderDetail", "Network Error: ${t.message}")
                }
            })
    }

    private fun displayOrderInfo(token: String, orderItems: List<OrderDetailData>) {

        for (orderItem in orderItems.take(1)) {
            // Extract product details
            val productName = orderItem.produk
            val productImage = orderItem.produk_image
            val productKategori = orderItem.produk_kategori
            val quantity = orderItem.quantity
            val totalPrice = orderItem.total_price
            val status = orderItem.status
            val date = orderItem.tanggal
            val orderId = orderItem.order_id
            val userName = orderItem.buyer.user.username
            val paymentMethod = orderItem.method?: "Bluetooth"
            val deliveryAddress = orderItem.buyer.alamat
            val deliveryCity = orderItem.buyer.kota
            val deliveryProvince = orderItem.buyer.provinsi
            val deliveryPostalCode = orderItem.buyer.kodepos

            findViewById<TextView>(R.id.deliveryAddress).text = "Delivered to: $deliveryAddress, $deliveryCity, $deliveryProvince, $deliveryPostalCode"
            findViewById<TextView>(R.id.orderDate).text = date
            findViewById<TextView>(R.id.orderFor).text = "#$orderId for $userName"
            findViewById<TextView>(R.id.paymentMethod).text = "Payment method: ${paymentMethod.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }}"
        }
    }

    private fun displayOrders(token: String, orderItems: List<OrderDetailData>) {
        val orderContainer = findViewById<LinearLayout>(R.id.productContainer)
        orderContainer.removeAllViews()

        // Initialize itemCount and subTotal
        var itemCount = 0
        var subTotal = 0
        val shippingFee = 300000 // Fixed shipping fee

        for (orderItem in orderItems.sortedByDescending { it.id }) {
            val orderItemView = layoutInflater.inflate(R.layout.invoice_product, orderContainer, false)

            // Extract product details
            val productName = orderItem.produk
            val productKategori = orderItem.produk_kategori
            val quantity = orderItem.quantity
            val totalPrice = orderItem.total_price
            val supplierName = orderItem.supplier

            // Update itemCount and subTotal
            itemCount += quantity.toInt()
            subTotal += totalPrice.toInt()

            // Set UI data for product name, category, quantity, and price
            orderItemView.findViewById<TextView>(R.id.productQty).text = "$quantity"
            orderItemView.findViewById<TextView>(R.id.productAmount).text = "Rp. ${formatWithDots(totalPrice)}"
            orderItemView.findViewById<TextView>(R.id.productSupplier).text = supplierName

            // Set product name and category
            val kategoriTextView = orderItemView.findViewById<TextView>(R.id.productName)
            kategoriTextView.text = when {
                productKategori == "Ayam" || productKategori == "Bebek" -> "$productName - Poultry"
                productKategori.contains("Telur", true) -> "$productName - Egg"
                productKategori.contains("Potong", true) -> "$productName - Meat"
                else -> "$productName - Seed"
            }

            // Add the view to the container
            orderContainer.addView(orderItemView)
        }

        // Calculate totalGrandPrice
        val totalGrandPrice = subTotal + shippingFee

        // Update UI
        val itemCountTextView = findViewById<TextView>(R.id.itemCount)
        itemCountTextView.text = "$itemCount Items"

        val subTotalTextView = findViewById<TextView>(R.id.subTotal)
        subTotalTextView.text = "Rp. ${formatWithDots(subTotal.toString())}"

        val shippingFeeTextView = findViewById<TextView>(R.id.shippingFee)
        shippingFeeTextView.text = "Rp. ${formatWithDots(shippingFee.toString())}"

        val totalGrandPriceTextView = findViewById<TextView>(R.id.totalPrice)
        totalGrandPriceTextView.text = "Rp. ${formatWithDots(totalGrandPrice.toString())}"
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
}