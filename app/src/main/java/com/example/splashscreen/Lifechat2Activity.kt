package com.example.splashscreen

import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class Lifechat2Activity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lifechat2)

        // Initialize views
        chatContainer = findViewById(R.id.chatContainer)
        scrollView = findViewById(R.id.scrollView) // Add an ID for the ScrollView in XML
        messageInput = findViewById(R.id.messageInput) // Add an ID for the input EditText in XML
        sendButton = findViewById(R.id.sendButton)
        val receiverImageView = findViewById<CircleImageView>(R.id.receiverPfp)

        val receiverName = intent.getStringExtra("receiverName")
        val receiverImage = intent.getStringExtra("receiverImage")

        findViewById<TextView>(R.id.receiverName).text = receiverName
        findViewById<ImageButton>(R.id.backbtn).setOnClickListener {
            finish()
        }

        if (receiverImage != null) {
            loadImageFromSupabase(receiverImage,receiverImageView)
        }

        // Handle send button click
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()

            if (messageText.isNotEmpty()) {
                // Add message to chat as sender
                addChatBubble(messageText, isSender = true)

                // Simulate receiver response (for demonstration purposes)
                addChatBubble("Yaya saya setuju", isSender = false)

                // Clear the input field
                messageInput.text.clear()
            }
        }
    }

    private fun addChatBubble(message: String, isSender: Boolean) {
        // Inflate the correct chat bubble layout
        val inflater = LayoutInflater.from(this)
        val bubble = if (isSender) {
            inflater.inflate(R.layout.sender_bubble, chatContainer, false)
        } else {
            inflater.inflate(R.layout.receiver_bubble, chatContainer, false)
        }

        // Set message text
        bubble.findViewById<TextView>(if (isSender) R.id.sender else R.id.receiver).text = message

        // Add the bubble to the chat container
        chatContainer.addView(bubble)

        // Scroll to the bottom to show the latest message
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun getStoredToken(): String? {
        val sharedPreferences = getSharedPreferences("user_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("TOKEN", null)  // Returns null if no token is stored
    }

    // load user's avatar from supabase
    private fun loadImageFromSupabase(filePath: String, imageView: CircleImageView) {
        lifecycleScope.launch {
            try {
                // Construct the public URL to the object in the storage bucket
                val imageUrl = "https://hbssyluucrwsbfzspyfp.supabase.co/storage/v1/object/public/avatar/$filePath/1.jpg?t=${System.currentTimeMillis()}"

                // Use Glide to load the image into the ImageView
                Glide.with(this@Lifechat2Activity)
                    .load(imageUrl)
                    .override(100, 100)
                    .placeholder(R.drawable.fotoprofil) // Add a placeholder image
                    .error(R.drawable.fotoprofil) // Add an error image
                    .into(imageView)
                Log.d("ImageLoad", "Image loaded successfully from $imageUrl")
            } catch (e: Exception) {
                Log.e("ImageLoadError", "Failed to load image: ${e.message}")
            }
        }
    }
}
