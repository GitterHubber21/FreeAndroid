package com.example.free


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LicenseActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var link: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.licenses) // Make sure the layout XML is named correctly

        // Initialize views
        backButton = findViewById(R.id.back_button)
        link = findViewById(R.id.link)

        // Set up click listener for backButton
        backButton.setOnClickListener {
            finish() // This finishes the activity when the back button is clicked
        }

        // Set up click listener for link
        link.setOnClickListener {
            // Open the URL in a web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tailwindlabs/heroicons"))
            startActivity(intent)
        }
    }
}
