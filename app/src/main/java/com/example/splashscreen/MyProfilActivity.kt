package com.example.splashscreen

import android.content.Intent
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
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import retrofit2.Response

class MyProfilActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

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
        lifecycleScope.launch {
            // get user id from auth email and load avatar
            val userId = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
            if (userId != null) {
                loadImageFromSupabase("$userId/1.jpg")
            }

            if (userId != null) {
                fetchBuyerDetails(userId.toLong())
            }
        }

        findViewById<ImageButton>(R.id.editProfil).setOnClickListener {
            startActivity(Intent(this, EditProfilActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            startActivity(Intent(this, ProfilActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        // Re-fetch buyer details
        lifecycleScope.launch {
            val userEmail = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
            if (userEmail != null) {
                // Get the buyer details again after the profile update
                val userId = userEmail.toLong()
                val buyerDetailsResponse = RetrofitClient.instance.getBuyerDetails(mapOf("p_uid" to userId))
                if (buyerDetailsResponse.isSuccessful) {
                    val buyerDetails = buyerDetailsResponse.body()?.firstOrNull()

                    if (buyerDetails != null) {
                        // Display the updated country and city
                        val negara = buyerDetails.negara ?: "Not available"
                        val kota = buyerDetails.kota ?: "Not available"
                        val firstName = buyerDetails.firstname ?: "Not avalaible"
                        val lastName = buyerDetails.lastname ?: "Not avalaible"
                        val province = buyerDetails.provinsi ?: "Not available"
                        val alamat = buyerDetails.alamat ?: "Not available"
                        val number = buyerDetails.telepon ?: "Not available"
                        val kodepos = buyerDetails.kodepos ?: "Not available"

                        findViewById<TextView>(R.id.phoneNumber).text = "$number"
                        findViewById<TextView>(R.id.alamat).text = "$alamat"
                        findViewById<TextView>(R.id.province).text = "$province"
                        findViewById<TextView>(R.id.city).text = "$kota"
                        findViewById<TextView>(R.id.country).text = "$negara"
                        findViewById<TextView>(R.id.FirstName).text = "$firstName "
                        findViewById<TextView>(R.id.LastName).text = "$lastName"

                        loadImageFromSupabase("$userEmail/1.jpg")
                    }
                } else {
                    Log.e("ProfilActivity", "Failed to retrieve buyer details: ${buyerDetailsResponse.errorBody()?.string()}")
                }
            }
        }
    }

    private fun fetchBuyerDetails(userId: Long) {
        lifecycleScope.launch {
            try {
                val response: Response<List<BuyerDetails>> = RetrofitClient.instance.getBuyerDetails(mapOf("p_uid" to userId))
                if (response.isSuccessful) {
                    val detailsList = response.body()
                    if (!detailsList.isNullOrEmpty()) {
                        val details = detailsList[0]
                        // Populate the EditText fields with current details
                        firstName.setText(details.firstname ?: "")
                        lastName.setText(details.lastname ?: "")
                        alamat.setText(details.alamat ?: "")
                        phoneNumber.setText(details.telepon?.toString() ?: "")
                        kota.setText(details.kota ?: "")
                        kodePos.setText(details.kodepos ?: "")
                        provinsi.setText(details.provinsi ?: "")
                        country.setText(details.negara ?: "")
                        lifecycleScope.launch {
                            val userId = supabase.auth.retrieveUserForCurrentSession().email ?: return@launch
                            email.setText(userId)
                        }

                        fullName.setText("${details.firstname ?: ""} ${details.lastname ?: ""}")
                    }
                } else {
                    Toast.makeText(this@MyProfilActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyProfilActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // get user's id by email to get their avatar's path
    private suspend fun getUserIdByEmail(email: String): Int? {
        val requestBody = mapOf("user_email" to email)
        val response: Response<Int> = RetrofitClient.instance.getUserIdByEmail(requestBody)

        if (response.isSuccessful) {
            Log.d("APIResponse", "User ID retrieved: ${response.body()}")
            return response.body() // Return the user ID directly
        } else {
            Log.e("APIError", "Failed to retrieve user ID: ${response.errorBody()?.string()}")
            return null
        }
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