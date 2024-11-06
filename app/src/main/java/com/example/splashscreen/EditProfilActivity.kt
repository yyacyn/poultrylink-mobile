package com.example.splashscreen

import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
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
import retrofit2.Response

class EditProfilActivity : AppCompatActivity() {

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    private lateinit var lastName: EditText
    private lateinit var firstName: EditText
    private lateinit var phoneNumber: EditText
    private lateinit var country: EditText
    private lateinit var provinsi: EditText
    private lateinit var kota: EditText
    private lateinit var kodePos: EditText
    private lateinit var alamat: EditText
    private lateinit var btnSave: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.edit_profil)

        lastName = findViewById(R.id.LastName)
        firstName = findViewById(R.id.FirstName)
        phoneNumber = findViewById(R.id.PhoneNumber)
        country = findViewById(R.id.Country)
        provinsi = findViewById(R.id.Provinsi)
        kota = findViewById(R.id.Kota)
        kodePos = findViewById(R.id.KodePos)
        alamat = findViewById(R.id.Street)
        btnSave = findViewById(R.id.buttonSave)

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

            btnSave.setOnClickListener {
                val uid = userId/* Your logic to retrieve the user's ID */
                val userLastName = lastName.text.toString()
                val userFirstName = firstName.text.toString()
                val userPhoneNumber = phoneNumber.text.toString()
                val userCountry = country.text.toString()
                val userProvinsi = provinsi.text.toString()
                val userKota = kota.text.toString()
                val userKodePos = kodePos.text.toString()
                val userAlamat = alamat.text.toString()

                if (uid != null) {
                    updateProfile(uid.toLong(), userFirstName, userLastName, userPhoneNumber, userCountry, userProvinsi, userKota, userKodePos, userAlamat)
                }
            }
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
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
                Glide.with(this@EditProfilActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(findViewById<CircleImageView>(R.id.user_pfp))
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }

    private fun updateProfile(id: Long, firstName: String, lastName: String, phoneNumber: String, country: String, provinsi: String, kota: String, kodePos: String, alamat: String) {
        val profileData = UpdateBuyer(
            p_uid = id,
            p_firstname = firstName,
            p_lastname = lastName,
            p_alamat = alamat,
            p_telepon = phoneNumber.toLongOrNull() ?: 0L,
            p_kota = kota,
            p_kodepos = kodePos,
            p_provinsi = provinsi,
            p_negara = country
        )

        lifecycleScope.launch {
            try {
                val response: Response<Boolean> = RetrofitClient.instance.updateUserProfile(profileData)
                if (response.isSuccessful && response.body() == true) {
                    Toast.makeText(this@EditProfilActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EditProfilActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfilActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    }
                } else {
                    Toast.makeText(this@EditProfilActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfilActivity, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
