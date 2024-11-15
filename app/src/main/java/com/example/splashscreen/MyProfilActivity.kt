package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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

class MyProfilActivity : AppCompatActivity() {


    private lateinit var lastName: TextView
    private lateinit var fullName: TextView
    private lateinit var firstName: TextView
    private lateinit var phoneNumber: TextView
    private lateinit var country: TextView
    private lateinit var provinsi: TextView
    private lateinit var kota: TextView
    private lateinit var kodePos: TextView
    private lateinit var alamat: TextView
    private lateinit var email: TextView
    private lateinit var btnSave: Button
    private lateinit var imageInput: CircleImageView
    private lateinit var changePfpButton: ImageButton



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.my_profil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lastName = findViewById(R.id.LastName)
        firstName = findViewById(R.id.FirstName)
        phoneNumber = findViewById(R.id.phoneNumber)
        country = findViewById(R.id.country)
        provinsi = findViewById(R.id.province)
        kota = findViewById(R.id.city)
        kodePos = findViewById(R.id.kodepos)
        alamat = findViewById(R.id.alamat)
        fullName = findViewById(R.id.FullName)
        email = findViewById(R.id.userEmail)

        // user greeting

        findViewById<ImageButton>(R.id.editProfil).setOnClickListener {
            startActivity(Intent(this, EditProfilActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }


        val token = "Bearer ${getStoredToken().toString()}"

        getProfile(token)
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    override fun onResume() {
        super.onResume()

        val token = "Bearer ${getStoredToken().toString()}"
        getProfile(token)


    }

    private fun getProfile(token: String?) {
        RetrofitClient.instance.getProfile(token ?: "")
            .enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {

                        val buyerData = response.body()?.data
                        val username = buyerData?.user?.username ?: "User"
                        val userkota = buyerData?.kota ?: "Not available"
                        val usernegara = buyerData?.negara ?: "Not available"
                        val userId = buyerData?.id ?: 0
                        val firstName = buyerData?.firstname?:"Not Available"
                        val lastName = buyerData?.lastname?: "Not available"
                        val userEmail  = buyerData?.user?.email
                        val number = buyerData?.telepon ?: "Not available"
                        val alamat = buyerData?.alamat ?: "Not available"
                        val province = buyerData?.provinsi ?: "Not available"
                        val kota = buyerData?.kota ?: "Not available"
                        val kodepos = buyerData?.kodepos ?: "Not available"

                        val userPfp = findViewById<CircleImageView>(R.id.profile_image)

                        loadImageFromSupabase("$userId/1.jpg")


                        findViewById<TextView>(R.id.phoneNumber).text = "$number"
                        findViewById<TextView>(R.id.alamat).text = "$alamat"
                        findViewById<TextView>(R.id.province).text = "$province"
                        findViewById<TextView>(R.id.city).text = "$kota"
                        findViewById<TextView>(R.id.country).text = "$usernegara"
                        findViewById<TextView>(R.id.FirstName).text = "$firstName "
                        findViewById<TextView>(R.id.LastName).text = "$lastName"
                        findViewById<TextView>(R.id.userEmail).text = "$userEmail"
                        findViewById<TextView>(R.id.kodepos).text = "$kodepos"
                        findViewById<TextView>(R.id.FullName).text = "$firstName $lastName"

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
                Glide.with(this@MyProfilActivity)
                    .load(imageUrl)
                    .override(200, 200)
                    .transition(DrawableTransitionOptions.withCrossFade())
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