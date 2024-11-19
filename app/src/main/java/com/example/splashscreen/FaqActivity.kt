package com.example.splashscreen

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FaqActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.faq)
        enableEdgeToEdge()

        // Setup FAQ 1
        setupFAQ(
            findViewById(R.id.headerLayout1),
            findViewById(R.id.contentTextView1),
            findViewById(R.id.arrowImageView1)
        )

        // Setup FAQ 2
        setupFAQ(
            findViewById(R.id.headerLayout2),
            findViewById(R.id.contentTextView2),
            findViewById(R.id.arrowImageView2)
        )

        // Setup FAQ 3
        setupFAQ(
            findViewById(R.id.headerLayout3),
            findViewById(R.id.contentTextView3),
            findViewById(R.id.arrowImageView3)
        )
    }

    private fun setupFAQ(headerLayout: LinearLayout, contentTextView: TextView, arrowImageView: ImageView) {
        var isExpanded = false
        headerLayout.setOnClickListener {
            isExpanded = !isExpanded
            contentTextView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrowImageView.setImageResource(if (isExpanded) R.drawable.arrow_up else R.drawable.arrow_down)
        }
    }
}