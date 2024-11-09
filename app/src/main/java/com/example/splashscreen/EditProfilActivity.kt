package com.example.splashscreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.ByteArrayOutputStream

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
    private lateinit var imageInput: CircleImageView
    private lateinit var changePfpButton: ImageButton

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1
    }


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
        imageInput = findViewById(R.id.user_pfp)

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

        changePfpButton = findViewById(R.id.changePfpButton)
        changePfpButton.setOnClickListener {
            openImagePicker()
        }
    }


    private fun openImagePicker() {
        // Create intent to show all available image sources
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            // This flag will show the device folders instead of recent files
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        try {
            // Create a chooser to show all available options
            val chooserIntent = Intent.createChooser(intent, "Select a photo")
            startActivityForResult(chooserIntent, REQUEST_CODE_PICK_IMAGE)
        } catch (e: Exception) {
            Toast.makeText(this, "No app can handle this action", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // Display the selected image in the CircleImageView
                    imageInput.setImageURI(uri)

                    // Upload the selected image to Supabase
                    uploadProfilePicture(uri)
                } catch (e: Exception) {
                    Log.e("ImageSelection", "Error handling selected image: ${e.message}")
                    Toast.makeText(
                        this,
                        "Failed to load selected image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Replace this function to accept Uri instead of drawableId
    private fun getDrawableAsByteArray(imageUri: Uri): ByteArray? {
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("ImageConversionError", "Error converting image: ${e.message}")
            null
        }
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        lifecycleScope.launch {
            try {
                val userId = getUserIdByEmail(supabase.auth.retrieveUserForCurrentSession().email ?: return@launch)
                if (userId != null) {
                    val imageData = getDrawableAsByteArray(imageUri)
                    val filePath = "$userId/1.jpg"

                    if (imageData != null) {
                        val storage = supabase.storage["avatar"]

                        // Delete the old image if it exists before uploading the new one
                        storage.delete(filePath)  // Ensure old image is removed

                        // Upload new image and check result
                        storage.upload(filePath, imageData)
                        loadImageFromSupabase(filePath)
                    }
                }
            } catch (e: Exception) {
                Log.e("UploadError", "Error uploading profile picture: ${e.message}")
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
