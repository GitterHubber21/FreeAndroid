package com.example.free

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class AddictionActivity : AppCompatActivity() {


    private lateinit var buttonOvereating: Button
    private lateinit var buttonSmoking: Button
    private lateinit var buttonSocialMedia: Button
    private lateinit var buttonAlcohol: Button
    private lateinit var buttonSubmit: Button
    private lateinit var customAddictionInput: EditText
    private var selectedButton: Button? = null // Track selected addiction button, nullable for no selection
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addiction_choose)

        // Initialize buttons

        buttonOvereating = findViewById(R.id.button_overeating)
        buttonSmoking = findViewById(R.id.button_smoking)
        buttonSocialMedia = findViewById(R.id.button_social_media)
        buttonAlcohol = findViewById(R.id.button_alcohol)
        buttonSubmit = findViewById(R.id.button_submit)
        customAddictionInput = findViewById(R.id.custom_addiction_input)

        // Set click listeners for addiction buttons

        setAddictionButtonClickListener(buttonOvereating)
        setAddictionButtonClickListener(buttonSmoking)
        setAddictionButtonClickListener(buttonSocialMedia)
        setAddictionButtonClickListener(buttonAlcohol)

        // Add text listener to custom input to deselect any selected button when typing begins
        customAddictionInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                deselectSelectedButton() // Deselect any selected button
            }

            override fun afterTextChanged(s: Editable) {}
        })

        // Set click listener for submit button
        buttonSubmit.setOnClickListener { handleSubmit() }
    }

    // Method to set click listener for each addiction button
    private fun setAddictionButtonClickListener(button: Button) {
        button.setOnClickListener {
            deselectSelectedButton() // Deselect previous button, if any
            selectedButton = button // Set current button as selected
            selectedButton?.setBackgroundResource(R.drawable.button_selected_gradient) // Highlight selected button
            customAddictionInput.text.clear() // Clear custom input field
        }
    }

    // Helper function to deselect the currently selected button
    private fun deselectSelectedButton() {
        selectedButton?.setBackgroundResource(R.drawable.button_gradient) // Reset background of previous button
        selectedButton = null // Clear the selected button reference
    }

    // Handle submit button click
    private fun handleSubmit() {
        val selectedAddiction: String

        // Check if an addiction button is selected
        if (selectedButton != null) {
            selectedAddiction = selectedButton?.text.toString()
        } else {
            // If no button is selected, check if custom input is provided
            selectedAddiction = customAddictionInput.text.toString().trim()
            if (TextUtils.isEmpty(selectedAddiction)) {
                Toast.makeText(this, "Please select or enter an addiction.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Save state in SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("addiction", selectedAddiction)
        editor.putBoolean("addiction_choice_complete", true)
        editor.putBoolean("is_authenticated", true)
        editor.apply()

        // Proceed to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("selectedAddiction", selectedAddiction)
        startActivity(intent)
        finish()
    }
}

