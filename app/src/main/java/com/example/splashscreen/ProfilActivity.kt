package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.homepage.HomeActivity
import com.google.api.Distribution.BucketOptions.Linear
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.net.URL

class ProfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val greetUser = findViewById<TextView>(R.id.username)
        val userPfp = findViewById<CircleImageView>(R.id.profile_image)
        val userLocation = findViewById<TextView>(R.id.user_location)

        val token = "Bearer ${getStoredToken().toString()}"

        getProfile(token,greetUser, userLocation, userPfp)

        Navigation()

    }

    override fun onResume() {
        super.onResume()

        val greetUser = findViewById<TextView>(R.id.username)
        val userPfp = findViewById<CircleImageView>(R.id.profile_image)
        val userLocation = findViewById<TextView>(R.id.user_location)

        // Retrieve the token and update profile
        val token = "Bearer ${getStoredToken().toString()}"
        getProfile(token, greetUser, userLocation, userPfp)
    }


    // nav
    private fun Navigation(){
        val btnHome = findViewById<ImageButton>(R.id.btnhome)
        val btnMarket = findViewById<ImageButton>(R.id.btnmarket)
        val btnHistory = findViewById<ImageButton>(R.id.btnhistory)
        val btnProfil = findViewById<ImageButton>(R.id.btnprofil)
        val btnEditProfil = findViewById<LinearLayout>(R.id.editProfile)
//        val btnOpenStore = findViewById<Button>(R.id.open_store_button)
        val btnNotif = findViewById<LinearLayout>(R.id.notif)
        val btnTransaction = findViewById<LinearLayout>(R.id.transactionHistory)
        val btnFaq = findViewById<LinearLayout>(R.id.faq)
        val btnLifechat = findViewById<LinearLayout>(R.id.lifeChat)
        val btnLogout = findViewById<Button>(R.id.logout_button)
//        val btnBack = findViewById<ImageButton>(R.id.btn_back)

        btnHome.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        window.navigationBarColor = resources.getColor(R.color.orange)

        btnHistory.setOnClickListener {
            startActivity(Intent(this, CompletePaymentActivity::class.java))
        }

        btnProfil.setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }

        btnEditProfil.setOnClickListener {
            startActivity(Intent(this, MyProfilActivity::class.java))
        }

        btnLifechat.setOnClickListener {
            startActivity(Intent(this, Lifechat2Activity::class.java))
        }

        btnNotif.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

//        btnOpenStore.setOnClickListener {
//            startActivity(Intent(this, TokoActivity::class.java))
//        }

        btnFaq.setOnClickListener {
            startActivity(Intent(this, FaqActivity::class.java))

        }

        btnTransaction.setOnClickListener {
            startActivity(Intent(this, CartCompleteActivity::class.java))
        }


//        btnBack.setOnClickListener {
//            startActivity(Intent(this, HomeActivity::class.java))
//        }

        btnLogout.setOnClickListener {
            val token = "Bearer ${getStoredToken().toString()}"

            Log.d("headerlogout", token)

            RetrofitClient.instance.logoutMobile(token).enqueue(object : Callback<Map<String, Boolean>> {
                override fun onResponse(call: Call<Map<String, Boolean>>, response: Response<Map<String, Boolean>>) {
                    Log.d("LogoutResponse", "Response: $response, Body: ${response.body()}, ErrorBody: ${response.errorBody()?.string()}")

                    if (response.isSuccessful) {
                        val responselogout = response.body()
                        val success = responselogout?.get("success") == true
                        if (success) {
                            Toast.makeText(this@ProfilActivity, "Logout successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ProfilActivity, PilihanLoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@ProfilActivity, "Logout failed, please try again", Toast.LENGTH_SHORT).show()
                            Log.e("LogoutError", "Logout returned false")
                        }
                    } else {
                        Log.e("LogoutError", "Failed to logout: ${response.errorBody()?.string()}")
                        Toast.makeText(this@ProfilActivity, "Logout failed, server error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Boolean>>, t: Throwable) {
                    Log.e("LogoutError", "Network failure: ${t.message}")
                }
            })
        }
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }


    private fun getProfile(token: String?, greetUser: TextView, userLocation: TextView, userPfp: CircleImageView) {
        RetrofitClient.instance.getProfile(token ?: "")
            .enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {
                        val buyerData = response.body()?.data
                        val username = buyerData?.user?.username ?: "User"
                        val userkota = buyerData?.kota
                        val usernegara = buyerData?.negara
                        val userId = buyerData?.id ?: 0
                        val firstName = buyerData?.firstname?:"Not Available"
                        val lastName = buyerData?.lastname?:""
                        val userEmail  = buyerData?.user?.email

                        loadImageFromSupabase("$userId/1.jpg")

                        greetUser.text = "Hello, $username!"
                        findViewById<TextView>(R.id.FirstName).text = "$firstName "
                        findViewById<TextView>(R.id.LastName).text = "$lastName"
                        findViewById<TextView>(R.id.user_email).text = "$userEmail"
                        if (userkota.isNullOrEmpty() || usernegara.isNullOrEmpty()){
                            userLocation.text = "Somewhere"
                        } else{
                            findViewById<TextView>(R.id.user_location).text = "$userkota, $usernegara"
                            userLocation.text = "$userkota, $usernegara"
                        }
                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }

    // load user's avatar from supabase
    private fun loadImageFromSupabase(filePath: String) {
        lifecycleScope.launch {
            try {
                // Construct the public URL to the object in the storage bucket
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath?t=${System.currentTimeMillis()}"

                // Use Glide to load the image into the ImageView
                Glide.with(this@ProfilActivity)
                    .load(imageUrl)
                    .override(200, 200)
//                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(findViewById<CircleImageView>(R.id.profile_image))
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }
}