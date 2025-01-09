package com.example.free

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context

import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar


class SettingsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var resetApp: TextView
    private lateinit var resetPin: TextView
    private lateinit var instagram: TextView
    private lateinit var website: TextView
    private lateinit var email: TextView
    private lateinit var rateButton: TextView
    private lateinit var cancelButton: Button
    private lateinit var confirmButton: Button
    private lateinit var switch_button: Switch
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationManager: NotificationManager  // Add this to handle notifications
    private lateinit var licenseButton : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        setContentView(R.layout.activity_settings)

        // Find views by ID
        backButton = findViewById(R.id.back_button)
        resetApp = findViewById(R.id.reset_app)
        instagram = findViewById(R.id.instagram)
        website = findViewById(R.id.website)
        email = findViewById(R.id.email)
        rateButton = findViewById(R.id.rate_app)
        switch_button = findViewById(R.id.switch_notifications)
        resetPin = findViewById(R.id.reset_pin)
        licenseButton=findViewById(R.id.license)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        backButton.setOnClickListener {
            finish()
        }
        rateButton.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
        instagram.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.instagram.com/masterbrosdev")
            }
            startActivity(intent)
        }
        licenseButton.setOnClickListener {
            val intent = Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }

        website.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://brand.masterbros.dev")
            }
            startActivity(intent)
        }

        email.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:contact@masterbros.dev")
            }
            // Check if an email app is available before starting the activity
            startActivity(intent)
        }


        // Set switch to its saved state
        val switchState = sharedPreferences.getBoolean("switch_state", true)
        switch_button.isChecked = switchState

        // Enable/Disable notifications based on saved state
        if (switchState) {
            enableNotifications2()
            scheduleDailyWrapUpReminder()
        } else {
            disableNotifications2()
            cancelDailyWrapup()
        }

        // Handle switch state change
        switch_button.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().apply {
                putBoolean("switch_state", isChecked)
                apply()
            }

            // Enable or disable notifications based on the switch state
            if (isChecked) {
                enableNotifications()
                scheduleDailyWrapUpReminder()
            } else {
                disableNotifications()
                cancelDailyWrapup()
            }
        }

        // Other button actions...

        resetApp.setOnClickListener {
            showConfirmationDialog()
        }

        resetPin.setOnClickListener {
            showConfirmationDialog2()
        }
    }

    private fun enableNotifications() {
        // Create notification channel if not already created
        createNotificationChannel()

        // Schedule hourly reminder if switch is turned on
        scheduleHourlyReminder()

        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
    }

    private fun disableNotifications() {
        // Cancel scheduled notifications if switch is turned off
        cancelHourlyReminder()

        Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
    }
    private fun enableNotifications2() {
        // Create notification channel if not already created
        createNotificationChannel()

        // Schedule hourly reminder if switch is turned on
        scheduleHourlyReminder()


    }

    private fun disableNotifications2() {
        // Cancel scheduled notifications if switch is turned off
        cancelHourlyReminder()


    }

    private fun createNotificationChannel() {
        val name = "Progress Reminder"
        val descriptionText = "Channel for progress reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("progress_reminder", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleHourlyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ProgressReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the alarm to trigger every hour
        val interval = 7200 * 1000L  // 1 minute in milliseconds
        val triggerTime = System.currentTimeMillis() + interval

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            interval,
            pendingIntent
        )
    }

    private fun cancelHourlyReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ProgressReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel the alarm
        alarmManager.cancel(pendingIntent)
    }
    private fun cancelDailyWrapup() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyWrapUpReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel the alarm
        alarmManager.cancel(pendingIntent)
    }
    private fun scheduleDailyWrapUpReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyWrapUpReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1, // Unique request code for wrap-up notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the alarm for 8:30 PM
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }



        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun showConfirmationDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)

        // Build the AlertDialog
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .setCancelable(true) // Allows the dialog to be cancelled
            .create()

        // Set up the custom layout components
        val cancelButton: Button = dialogView.findViewById(R.id.button_cancel)
        val confirmButton: Button = dialogView.findViewById(R.id.button_confirm)

        // Set click listeners for the buttons
        cancelButton.setOnClickListener {
            dialog.dismiss() // Dismiss the dialog if Cancel is clicked
        }

        confirmButton.setOnClickListener {
            // If "Yes" is clicked, start the new activity
            val intent = Intent(this, AppResetVerification::class.java)
            startActivity(intent)
            dialog.dismiss() // Dismiss the dialog after starting the activity
        }

        // Show the dialog
        dialog.show()
    }

    private fun showConfirmationDialog2() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_pin_reset, null)

        // Build the AlertDialog
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(dialogView)
            .setCancelable(true) // Allows the dialog to be cancelled
            .create()

        // Set up the custom layout components
        val cancelButton: Button = dialogView.findViewById(R.id.button_cancel)
        val confirmButton: Button = dialogView.findViewById(R.id.button_confirm)

        // Set click listeners for the buttons
        cancelButton.setOnClickListener {
            dialog.dismiss() // Dismiss the dialog if Cancel is clicked
        }

        confirmButton.setOnClickListener {
            // If "Yes" is clicked, start the new activity
            val intent = Intent(this, PinResetVerification::class.java)
            startActivity(intent)
            dialog.dismiss() // Dismiss the dialog after starting the activity
        }

        // Show the dialog
        dialog.show()
    }
}
