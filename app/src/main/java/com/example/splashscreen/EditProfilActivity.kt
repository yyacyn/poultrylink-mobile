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
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.textfield.TextInputEditText
import com.yourapp.network.RetrofitClient
import de.hdodenhof.circleimageview.CircleImageView
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.FileNotFoundException
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL

class EditProfilActivity : AppCompatActivity() {

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
    private var imageUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1
    }

    private var buyerId: Int = 0


    private val supabase = createSupabaseClient(
        supabaseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhic3N5bHV1Y3J3c2JmenNweWZwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk2NTU4OTEsImV4cCI6MjA0NTIzMTg5MX0.o6fkro2tPKFoA9sxAp1nuseiHRGiDHs_HI4-ZoqOTfQ"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
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

        val token = "Bearer ${getStoredToken()}"

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }


        // Set the click listener for Save button here
        btnSave.setOnClickListener {
            handleSaveClick(token)
        }

        changePfpButton = findViewById(R.id.changePfpButton)
        changePfpButton.setOnClickListener {
            openImagePicker()
        }

        getProfile(token)
    }

    // Retrieve the token from SharedPreferences
    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
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

    private fun uploadImageToSupabase(imageUri: Uri, buyerId: Int) {
        lifecycleScope.launch {
            try {
                val imageData = getDrawableAsByteArray(imageUri)
                val filePath = "$buyerId/1.jpg"  // Use buyerId as folder name

                if (imageData != null) {
                    val storage = supabase.storage["avatar"]

                    // Delete old image if it exists
                    storage.delete(filePath)

                    // Upload new image
                    storage.upload(filePath, imageData)

                    // Load the image with cache refresh
                    loadImageFromSupabase(filePath)
                }
            } catch (e: Exception) {
                Log.e("UploadError", "Error uploading profile picture: ${e.message}")
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                imageUri = uri
                imageInput.setImageURI(uri)

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

    private fun handleSaveClick(token: String?) {
        val userLastName = lastName.text.toString()
        val userFirstName = firstName.text.toString()
        val userPhoneNumber = phoneNumber.text.toString()
        val userCountry = country.text.toString()
        val userProvinsi = provinsi.text.toString()
        val userKota = kota.text.toString()
        val userKodePos = kodePos.text.toString()
        val userAlamat = alamat.text.toString()

        updateProfile(token, userFirstName, userLastName, userPhoneNumber, userCountry, userProvinsi, userKota, userKodePos, userAlamat)

        // Check if imageUri is not null, meaning a new image has been selected
        imageUri?.let {
            uploadImageToSupabase(it, buyerId)  // Upload the image if a new one was selected
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
                    .override(200, 200)
                    .placeholder(R.drawable.fotoprofil)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(findViewById<CircleImageView>(R.id.user_pfp))
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }

    private fun getProfile(token: String?) {

        RetrofitClient.instance.getProfile(token ?: "")
            .enqueue(object : Callback<BuyerResponse> {
                override fun onResponse(call: Call<BuyerResponse>, response: Response<BuyerResponse>) {
                    if (response.isSuccessful) {

                        val buyerData = response.body()?.data
                        buyerId = (buyerData?.id ?: 0).toInt()
                        val username = buyerData?.user?.username ?: "User"
                        val userkota = buyerData?.kota ?: "Not available"
                        val usernegara = buyerData?.negara ?: "Not available"
                        val userId = buyerData?.id ?: 0
                        val firstname = buyerData?.firstname?:"Not Available"
                        val lastname = buyerData?.lastname?: "Not available"
                        val userEmail  = buyerData?.user?.email
                        val number = buyerData?.telepon ?: "Not available"
                        val useralamat = buyerData?.alamat ?: "Not available"
                        val province = buyerData?.provinsi ?: "Not available"
                        val kodepos = buyerData?.kodepos ?: "Not available"

                        val userPfp = findViewById<CircleImageView>(R.id.user_pfp)

                        loadImageFromSupabase("$userId/1.jpg")

                        firstName.hint = firstname ?: ""
                        lastName.hint = lastname
                        alamat.hint = useralamat
                        phoneNumber.hint = number
                        kota.hint = userkota
                        kodePos.hint = kodepos
                        provinsi.hint = province
                        country.hint = usernegara

                    } else {
                        // Handle error cases
                    }
                }

                override fun onFailure(call: Call<BuyerResponse>, t: Throwable) {
                    // Handle network errors
                }
            })
    }


    private fun updateProfile(token: String?, firstName: String, lastName: String, phoneNumber: String, country: String, provinsi: String, kota: String, kodePos: String, alamat: String) {
        lifecycleScope.launch {
            try {

                val request = UpdateProfileRequest(
                    firstname = firstName,   // Replace with the actual value or null
                    lastname = lastName,       // Null for no value
                    alamat = phoneNumber,
                    telepon = country,
                    kota = provinsi,
                    kodepos = kota,
                    provinsi = kodePos,
                    negara = alamat
                )

                // Make the update profile request with Retrofit
                val response: Response<Any> = RetrofitClient.instance.updateProfile(
                    "$token", request)

                if (response.isSuccessful) {
                    Log.i("ProfileUpdate", "Profile updated successfully.")
                    Toast.makeText(this@EditProfilActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    getProfile(token)

                } else {
                    Log.e("ProfileUpdate", "Failed to update profile: ${response.code()} - ${response.message()}")
                    Log.e("ProfileUpdate", "Response body: ${response.errorBody()?.string()}")
                    Toast.makeText(this@EditProfilActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: FileNotFoundException) {
                Log.e("ProfileUpdate", "File not found for image URI: ${imageUri?.path}")
                Toast.makeText(this@EditProfilActivity, "Image file not found", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("ProfileUpdate", "I/O error: ${e.message}")
                Toast.makeText(this@EditProfilActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ProfileUpdate", "Unexpected error updating profile: ${e.message}")
                Toast.makeText(this@EditProfilActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
