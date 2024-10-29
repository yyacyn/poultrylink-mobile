package com.example.splashscreen

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditProfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.edit_profil)
        }

    fun onRadioButtonClick(view: View) {
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val selectedRadioButton = findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
        Toast.makeText(this, "${selectedRadioButton.text} is selected", Toast.LENGTH_SHORT).show()
    }
}
