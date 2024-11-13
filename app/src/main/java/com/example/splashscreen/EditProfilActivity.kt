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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                imageUri = uri
                imageInput.setImageURI(uri)
            }
        }
    }


    // Modify your getDrawableAsByteArray function to return a File
    private fun getDrawableAsFile(imageUri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Create a temporary file to store the image
            val tempFile = File.createTempFile("avatar_", ".jpg", cacheDir)
            tempFile.deleteOnExit()

            // Write the bitmap to the temp file
            val outputStream = tempFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()

            tempFile
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

        // Check if an image URI is selected
        if (imageUri != null) {
            val avatarFile = getDrawableAsFile(imageUri!!)

            if (avatarFile != null) {
                updateProfile(
                    token,
                    userFirstName,
                    userLastName,
                    userPhoneNumber,
                    userCountry,
                    userProvinsi,
                    userKota,
                    userKodePos,
                    userAlamat,
                    avatarFile
                )
            } else {
                // Handle error for missing avatar
                Toast.makeText(this, "Error: Avatar image is missing", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle case where no image is selected
            updateProfile(
                token,
                userFirstName,
                userLastName,
                userPhoneNumber,
                userCountry,
                userProvinsi,
                userKota,
                userKodePos,
                userAlamat,
                null // Send null if no image is selected
            )
        }
    }



    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView, forceRefresh: Boolean = false) {
        lifecycleScope.launch {
            try {
                val baseUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath"
                val imageUrl = if (forceRefresh) {
                    "$baseUrl?t=${System.currentTimeMillis()}"
                } else {
                    baseUrl
                }

                // Directly load image from URL as Bitmap
                withContext(Dispatchers.IO) {
                    val urlConnection = URL(imageUrl).openConnection()
                    val originalBitmap = BitmapFactory.decodeStream(urlConnection.getInputStream())

                    // Resize the image
                    val desiredWidth = 200 // Set the desired width
                    val desiredHeight = 200 // Set the desired height
                    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, true)

                    // Compress the image
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream) // Set the quality (0-100)
                    val compressedByteArray = outputStream.toByteArray()

                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.size))
                    }
                }

                Log.d("ImageLoaded", "Image loaded from: $imageUrl")
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

                        loadImageFromSupabase("$userId/1.jpg", userPfp)

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


    private fun updateProfile(
        token: String?,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        country: String,
        provinsi: String,
        kota: String,
        kodePos: String,
        alamat: String,
        avatarFile: File?
    ) {
        lifecycleScope.launch {
            try {
                // Prepare avatar file part if it's available
                val avatarPart = avatarFile?.let { file ->
                    val requestBody = RequestBody.create(MediaType.parse("image/*"), file)
                    MultipartBody.Part.createFormData("avatar_path", file.name, requestBody)
                }

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

                    val intent = Intent(this@EditProfilActivity, MyProfilActivity::class.java)
                    startActivity(intent)
                    finish()
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
