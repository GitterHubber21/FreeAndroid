package com.example.free

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var buttonNext: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        buttonNext = findViewById(R.id.button_next)

        buttonNext.setOnClickListener {

            val editor = sharedPreferences.edit()
            editor.putBoolean("onboarding_completed", true)
            editor.apply()

            val intent = Intent(this, PinActivity::class.java)
            startActivity(intent)

            finish()
        }
    }
}


