package com.example.free

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher

class PINTextWatcher(
    private val currentView: EditText,
    private val nextView: EditText?,
    private val previousView: EditText?,
    private val isLastView: Boolean = false // Flag to check if it's the last EditText
) : TextWatcher, View.OnKeyListener {

    init {
        currentView.setOnKeyListener(this)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // No action needed
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s?.length == 1) {

            // Use a Handler to introduce a delay of 10 milliseconds
            Handler().postDelayed({
                // After 10 ms, move focus to the next EditText, unless it's the last one
                if (!isLastView) {
                    nextView?.requestFocus()
                }
            }, 10) // 10 ms delay
        }
    }

    override fun afterTextChanged(s: Editable?) {
        // No action needed
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && event?.action == KeyEvent.ACTION_DOWN) {
            if (currentView.text.isEmpty()) {
                // Move focus to the previous view if the current is empty
                previousView?.requestFocus()
                previousView?.setText("")
            }
        }
        return false
    }
}

class PinVerificationActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var pinInput1: EditText
    private lateinit var pinInput2: EditText
    private lateinit var pinInput3: EditText
    private lateinit var pinInput4: EditText
    private lateinit var pinInput5: EditText
    private lateinit var pinInput6: EditText

    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_verify)

        // Initialize EditTexts and Button
        pinInput1 = findViewById(R.id.pin_input1)
        pinInput2 = findViewById(R.id.pin_input2)
        pinInput3 = findViewById(R.id.pin_input3)
        pinInput4 = findViewById(R.id.pin_input4)
        pinInput5 = findViewById(R.id.pin_input5)
        pinInput6 = findViewById(R.id.pin_input6)
        submitButton = findViewById(R.id.pin_submit)

        // Set EditText dimensions to 11.67% of screen width and equal height
        setEditTextDimensions()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Set up the text watchers for each EditText
        pinInput1.addTextChangedListener(PINTextWatcher(pinInput1, pinInput2, null))
        pinInput2.addTextChangedListener(PINTextWatcher(pinInput2, pinInput3, pinInput1))
        pinInput3.addTextChangedListener(PINTextWatcher(pinInput3, pinInput4, pinInput2))
        pinInput4.addTextChangedListener(PINTextWatcher(pinInput4, pinInput5, pinInput3))
        pinInput5.addTextChangedListener(PINTextWatcher(pinInput5, pinInput6, pinInput4))
        pinInput6.addTextChangedListener(PINTextWatcher(pinInput6, null, pinInput5, isLastView = true))

        val storedPin = sharedPreferences.getString("user_pin", null)

        submitButton.setOnClickListener {
            val enteredPin = "${pinInput1.text}${pinInput2.text}${pinInput3.text}${pinInput4.text}${pinInput5.text}${pinInput6.text}"
            if (enteredPin.length == 6) {
                if (enteredPin == storedPin) {
                    // Mark the user as authenticated for this session
                    sharedPreferences.edit().apply {
                        putBoolean("is_authenticated", true)
                        apply()
                    }
                    Toast.makeText(this, "Successful authentication", Toast.LENGTH_SHORT).show()
                    // Redirect to MainActivity (the home screen)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close this activity
                } else {
                    Toast.makeText(this, "Incorrect PIN, please try again.", Toast.LENGTH_SHORT).show()
                    clearPinInputs()
                }
            } else {
                Toast.makeText(this, "Invalid PIN, please try again.", Toast.LENGTH_SHORT).show()
                clearPinInputs()
            }
        }
    }

    private fun setEditTextDimensions() {
        // Get the display metrics
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        // Calculate size for EditText (11.67% of the screen width)
        val size = metrics.widthPixels * 0.1167 // This is the size for both width and height

        // Set the dimensions for each EditText
        for (editText in listOf(pinInput1, pinInput2, pinInput3, pinInput4, pinInput5, pinInput6)) {
            val layoutParams = editText.layoutParams
            layoutParams.width = size.toInt() // Set width
            layoutParams.height = size.toInt() // Set height to be equal to width
            editText.layoutParams = layoutParams // Apply updated layout params
        }
    }

    private fun clearPinInputs() {
        pinInput1.setText("")
        pinInput2.setText("")
        pinInput3.setText("")
        pinInput4.setText("")
        pinInput5.setText("")
        pinInput6.setText("")
        pinInput1.requestFocus()
    }
}

