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
    private var selectedButton: Button? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addiction_choose)

        buttonOvereating = findViewById(R.id.button_overeating)
        buttonSmoking = findViewById(R.id.button_smoking)
        buttonSocialMedia = findViewById(R.id.button_social_media)
        buttonAlcohol = findViewById(R.id.button_alcohol)
        buttonSubmit = findViewById(R.id.button_submit)
        customAddictionInput = findViewById(R.id.custom_addiction_input)

        setAddictionButtonClickListener(buttonOvereating)
        setAddictionButtonClickListener(buttonSmoking)
        setAddictionButtonClickListener(buttonSocialMedia)
        setAddictionButtonClickListener(buttonAlcohol)


        customAddictionInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                deselectSelectedButton()
            }

            override fun afterTextChanged(s: Editable) {}
        })


        buttonSubmit.setOnClickListener { handleSubmit() }
    }


    private fun setAddictionButtonClickListener(button: Button) {
        button.setOnClickListener {
            deselectSelectedButton()
            selectedButton = button
            selectedButton?.setBackgroundResource(R.drawable.button_selected_gradient)
            customAddictionInput.text.clear()
        }
    }


    private fun deselectSelectedButton() {
        selectedButton?.setBackgroundResource(R.drawable.button_gradient)
        selectedButton = null
    }


    private fun handleSubmit() {
        val selectedAddiction: String


        if (selectedButton != null) {
            selectedAddiction = selectedButton?.text.toString()
        } else {

            selectedAddiction = customAddictionInput.text.toString().trim()
            if (TextUtils.isEmpty(selectedAddiction)) {
                Toast.makeText(this, "Please select or enter an addiction.", Toast.LENGTH_SHORT).show()
                return
            }
        }


        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("addiction", selectedAddiction)
        editor.putBoolean("addiction_choice_complete", true)
        editor.putBoolean("is_authenticated", true)
        editor.apply()


        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("selectedAddiction", selectedAddiction)
        startActivity(intent)
        finish()
    }
}

