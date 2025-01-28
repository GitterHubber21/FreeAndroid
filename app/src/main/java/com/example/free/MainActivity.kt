package com.example.free


import android.app.AlarmManager
import android.view.WindowManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.app.Dialog
import android.app.NotificationChannel
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.icu.text.SimpleDateFormat
import androidx.core.app.NotificationCompat
import android.widget.LinearLayout
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var streakText: TextView
    private lateinit var buttonOkay: Button
    private lateinit var buttonFailed: Button
    private lateinit var gestureDetector: GestureDetector
    private lateinit var settingsButton: ImageView
    private lateinit var vibrator: Vibrator
    private var streakCounter: Int = 0
    private var currentDay = 0
    private lateinit var currentDayView: TextView
    private var currentDayPressed = false
    private var currentMonth = 0
    private var currentYear = 0
    private lateinit var SwitchButton : Button
    private lateinit var Bars : ImageView
    private lateinit var AddNewAddiction : Button
    private var selectedAddiction: String = ""
    private lateinit var monthTextView: TextView
    private lateinit var nextMonth: ImageView
    private lateinit var previousMonth: ImageView


    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gestureDetector = GestureDetector(this, SwipeGestureListener())
        createNotificationChannel()
        scheduleHourlyReminder()
        scheduleDailyWrapUpReminder()

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)


        val onboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)

        val isPinSetupComplete = sharedPreferences.getBoolean("pin_setup_complete", false)
        val addictionChoiceComplete = sharedPreferences.getBoolean("addiction_choice_complete", false)

        val isAuthenticated = sharedPreferences.getBoolean("is_authenticated", false)
        val resetting = sharedPreferences.getBoolean("resetting", false)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


        if (!onboardingCompleted) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }


        if (!isPinSetupComplete&&!resetting) {
            val intent = Intent(this, PinActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        if (isPinSetupComplete&&!addictionChoiceComplete&&!resetting) {
            val intent = Intent(this, AddictionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        if (isPinSetupComplete && !isAuthenticated&&addictionChoiceComplete) {
            val intent = Intent(this, PinVerificationActivity::class.java)
            startActivity(intent)
            finish()
            return
        }


        setContentView(R.layout.activity_main)


        settingsButton = findViewById(R.id.gearIcon)


        calendarGrid = findViewById(R.id.calendarGrid)
        streakText = findViewById(R.id.streakText)
        buttonOkay = findViewById(R.id.button_okay)
        buttonFailed = findViewById(R.id.button_failed)

        calendarGrid.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }


        buttonOkay.setOnClickListener {
            handleButtonClick(success = true)
            if(!buttonOkay.isEnabled){
                vibratePhone()}
        }

        buttonFailed.setOnClickListener {
            handleButtonClick(success = false)

        }



        settingsButton.setOnClickListener {

            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }


        val calendar = Calendar.getInstance()
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)



        streakCounter = sharedPreferences.getInt("streak_counter", 0)

        updateStreakText(streakCounter)


        populateCalendarForCurrentMonth()
        loadStreakForAddiction()

        if(resetting){
            resetApp()
        }
        monthTextView = findViewById(R.id.currentMonthText)



        currentMonth = calendar.get(Calendar.MONTH)
        currentYear = calendar.get(Calendar.YEAR)


        updateMonthText()


        populateCalendarForCurrentMonth()


        findViewById<ImageView>(R.id.button_prevMonth).setOnClickListener {
            navigateToPreviousMonth()
        }

        findViewById<ImageView>(R.id.button_nextMonth).setOnClickListener {
            navigateToNextMonth()
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val textViewWidth = (screenWidth * 0.4375).toInt()
        buttonOkay.layoutParams.width = textViewWidth
        buttonOkay.requestLayout()
        buttonFailed.layoutParams.width = textViewWidth
        buttonFailed.requestLayout()




        SwitchButton = findViewById(R.id.switch_button)
        Bars=findViewById(R.id.bars)


        val addictionValue = sharedPreferences.getString("addiction", "Select Addiction") ?: "Select Addiction"
        SwitchButton.text = addictionValue


        SwitchButton.setOnClickListener {
            showAddictionSelectionDialog()
        }
        Bars.setOnClickListener{
            showAddictionSelectionDialog()
        }

        populateCalendarForCurrentMonth()

    }

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffX = e2!!.x - e1!!.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY)) {

                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            navigateToPreviousMonth()
                        } else {
                            navigateToNextMonth()
                        }
                        return true
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }
    override fun onStop() {
        super.onStop()

        sharedPreferences.edit().apply {
            putBoolean("is_authenticated", false)
            putBoolean("resetting", false)
            apply()
        }
    }

    private fun populateCalendarForCurrentMonth() {
        calendarGrid.removeAllViews()
        addDayNames()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDayOffset = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val dayViewSize = (screenWidth * 0.1167).toInt()
        var isCurrentDayNeutral = false
        var isCurrentDayFail=false

        for (day in 1..daysInMonth) {
            val dayView = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.day_background)
            }

            val params = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec((startDayOffset + day - 1) % 7)
                rowSpec = GridLayout.spec((startDayOffset + day - 1) / 7 + 1)
                width = dayViewSize
                height = dayViewSize
                setMargins(4, 4, 4, 4)
            }
            dayView.layoutParams = params

            val dateKey = getDateKey(day, currentMonth, currentYear, selectedAddiction)
            val dayStatus = sharedPreferences.getString("${selectedAddiction}_status_$dateKey", "neutral")

            when (dayStatus) {
                "Success" -> dayView.setBackgroundResource(R.drawable.success_day_background)
                "Failure" -> {dayView.setBackgroundResource(R.drawable.fail_day_background)
                    isCurrentDayFail=true
                    disableButtons()}
                "neutral" -> {
                    if (day == currentDay &&
                        currentMonth == Calendar.getInstance().get(Calendar.MONTH) &&
                        currentYear == Calendar.getInstance().get(Calendar.YEAR)
                    ) {
                        dayView.setBackgroundResource(R.drawable.current_day_background)
                        currentDayView = dayView
                        isCurrentDayNeutral = true
                    }
                }
            }

            calendarGrid.addView(dayView)
        }

        if (currentMonth == Calendar.getInstance().get(Calendar.MONTH) &&
            currentYear == Calendar.getInstance().get(Calendar.YEAR)
        ) {
            if (isCurrentDayNeutral) {
                enableButtons()
            }
            if(!isCurrentDayFail&&!isCurrentDayNeutral) {
                buttonOkay.isEnabled = false
                buttonFailed.isEnabled = true
            }
        } else {
            disableButtons()
        }
    }

    private fun addDayNames() {

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayNameSize = (screenWidth * 0.1167).toInt()


        for (i in dayNames.indices) {
            val dayNameView = TextView(this)
            dayNameView.text = dayNames[i]
            dayNameView.gravity = Gravity.CENTER
            dayNameView.setTextColor(Color.WHITE)


            dayNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)


            val params = GridLayout.LayoutParams()
            params.columnSpec = GridLayout.spec(i)
            params.rowSpec = GridLayout.spec(0)
            params.width = dayNameSize
            params.height = dayNameSize
            params.setMargins(4, 4, 4, 4)

            dayNameView.layoutParams = params
            calendarGrid.addView(dayNameView)
        }
    }


    private fun handleButtonClick(success: Boolean) {
        if (!currentDayPressed && ::currentDayView.isInitialized) {
            val dayStatus = if (success) "Success" else "Failure"
            val dateKey = getDateKey(currentDay, currentMonth, currentYear, selectedAddiction)

            val statusJson = sharedPreferences.getString("${selectedAddiction}_status", "{}")
            val statusMap = mutableMapOf<String, String>()


            try {
                val jsonObject = JSONObject(statusJson ?: "{}")
                jsonObject.keys().forEach { key ->
                    statusMap[key] = jsonObject.getString(key)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            statusMap[dateKey] = dayStatus


            val updatedJsonObject = JSONObject()
            statusMap.forEach { (key, value) ->
                updatedJsonObject.put(key, value)
            }


            sharedPreferences.edit().apply {
                putString("${selectedAddiction}_status", updatedJsonObject.toString())
                apply()
            }

            sharedPreferences.edit().apply {
                putString("${selectedAddiction}_status_$dateKey", dayStatus)
                apply()
            }

            if (success) {

                val lastSuccessDate = sharedPreferences.getString("${selectedAddiction}_last_success_date", null)

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, currentDay)
                }
                val currentDate = calendar.time

                if (lastSuccessDate != null) {
                    val lastSuccessCalendar = Calendar.getInstance()
                    lastSuccessCalendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastSuccessDate)!!

                    val isSameYear = calendar.get(Calendar.YEAR) == lastSuccessCalendar.get(Calendar.YEAR)
                    val isNextDayInSameYear = isSameYear &&
                            calendar.get(Calendar.DAY_OF_YEAR) == lastSuccessCalendar.get(Calendar.DAY_OF_YEAR) + 1

                    val isNextDayInNewYear = !isSameYear &&
                            calendar.get(Calendar.YEAR) == lastSuccessCalendar.get(Calendar.YEAR) + 1 &&
                            calendar.get(Calendar.DAY_OF_YEAR) == 1 &&
                            lastSuccessCalendar.get(Calendar.DAY_OF_YEAR) == lastSuccessCalendar.getActualMaximum(Calendar.DAY_OF_YEAR)

                    if (isNextDayInSameYear || isNextDayInNewYear) {
                        streakCounter++
                    } else {
                        streakCounter = 1
                    }
                } else {
                    streakCounter = 1
                }

                sharedPreferences.edit().apply {
                    putString("${selectedAddiction}_last_success_date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate))
                    apply()
                }
            } else {
                streakCounter = 0
                disableButtons()
            }

            sharedPreferences.edit().apply {
                putInt(getStreakKey(selectedAddiction), streakCounter)
                apply()
            }

            updateStreakText(streakCounter)
            if (success) {
                currentDayView.setBackgroundResource(R.drawable.success_day_background)
                Toast.makeText(this, "Great job!", Toast.LENGTH_SHORT).show()
                buttonOkay.isEnabled = false
                buttonFailed.isEnabled = true
            } else {
                currentDayView.setBackgroundResource(R.drawable.fail_day_background)
                Toast.makeText(this, "Don't give up, you got this!", Toast.LENGTH_SHORT).show()
                disableButtons()
            }
            populateCalendarForCurrentMonth()
        }
    }


    private fun updateStreakText(streak: Int) {
        if (streak > 0) {
            streakText.text = "Your current streak is $streak day."
        } else {
            streakText.text = getString(R.string.you_have_no_streak_yet)
        }
    }

    private fun enableButtons() {
        val dateKey = getDateKey(currentDay, currentMonth, currentYear, selectedAddiction)
        val dayStatus = sharedPreferences.getString("${selectedAddiction}_status_$dateKey", "neutral")

        buttonOkay.isEnabled = dayStatus == "neutral"
        buttonFailed.isEnabled = dayStatus == "success" || dayStatus == "neutral"
    }

    private fun disableButtons() {
        buttonOkay.isEnabled = false
        buttonFailed.isEnabled = false
    }

    private fun resetApp() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }

        streakCounter = 0
        streakText.text = "You have no streak yet."

        for (i in 0 until calendarGrid.childCount) {
            val dayView = calendarGrid.getChildAt(i) as TextView
            if (i + 1 == currentDay) {
                currentDayView = dayView
                dayView.setBackgroundResource(R.drawable.current_day_background)
            } else {
                dayView.setBackgroundResource(R.drawable.day_background)
            }
        }

        buttonOkay.isEnabled = true
        buttonFailed.isEnabled = true
        sharedPreferences.edit().apply {

            putBoolean("resetting", false)

            apply()
        }
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)

        finish()
    }

    private fun vibratePhone() {

        if (vibrator.hasVibrator()) {

            val vibrationEffect = VibrationEffect.createOneShot(100, 30)
            vibrator.vibrate(vibrationEffect)
        } else {

            vibrator.vibrate(100)
        }
    }
    private val monthsArray = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private fun navigateToPreviousMonth() {
        currentMonth--
        if (currentMonth < 0) {
            currentMonth = 11
            currentYear--
        }
        updateMonthText()
        populateCalendarForCurrentMonth()
    }

    private fun navigateToNextMonth() {
        currentMonth++
        if (currentMonth > 11) {
            currentMonth = 0
            currentYear++
        }
        updateMonthText()
        populateCalendarForCurrentMonth()
    }


    private fun updateMonthText() {

        val monthName = monthsArray[currentMonth]

        monthTextView.text = "$monthName $currentYear"
    }


    private fun getDateKey(day: Int, month: Int, year: Int, addiction: String): String {
        return "$year-$month-$day-$addiction"
    }

    private fun showAddictionSelectionDialog() {

        val addictionSet = sharedPreferences.getStringSet("addictions", null) ?: emptySet()


        val addictionNames = addictionSet.toMutableList()

        val storedAddiction = sharedPreferences.getString("addiction", null)

        if (storedAddiction != null && !addictionNames.contains(storedAddiction)) {
            addictionNames.add(storedAddiction)
            saveAddictionsToSharedPreferences(addictionNames)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_addiction_selection, null)


        val addictionListLayout: LinearLayout = dialogView.findViewById(R.id.addictionListLayout)

        val dialog = Dialog(this, R.style.TransparentDialog)
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        val params = dialog.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.BOTTOM
        dialog.window?.attributes = params

        addictionNames.forEach { addictionName ->
            val textView = TextView(this).apply {
                text = addictionName
                textSize = 24f
                setTextColor(Color.WHITE)
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            textView.setOnClickListener {
                SwitchButton.text = addictionName
                sharedPreferences.edit().apply {
                    putString("addiction", addictionName)
                    apply()
                }

                loadStreakForAddiction()


                dialog.dismiss()
            }


            addictionListLayout.addView(textView)
        }

        val addNewAddictionButton: Button = dialogView.findViewById(R.id.addNewAddictionButton)
        addNewAddictionButton.setOnClickListener {
            val intent = Intent(this, AddictionActivity::class.java)
            startActivity(intent)
        }

        dialog.show()
    }



    private fun saveAddictionsToSharedPreferences(addictions: List<String>) {
        val addictionSet = addictions.toSet()
        sharedPreferences.edit().apply {
            putStringSet("addictions", addictionSet)
            apply()
        }
    }

    private fun getStreakKey(addiction: String): String {
        return "streak_$addiction"
    }
    private fun loadStreakForAddiction() {
        selectedAddiction = sharedPreferences.getString("addiction", "Select Addiction") ?: "Select Addiction"
        streakCounter = sharedPreferences.getInt(getStreakKey(selectedAddiction), 0)
        updateStreakText(streakCounter)
        populateCalendarForCurrentMonth()
    }
    private fun createNotificationChannel() {
        val name = "Progress Reminder"
        val descriptionText = "Channel for progress reminders"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("progress_reminder", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        val interval = 7200 * 1000L
        val triggerTime = System.currentTimeMillis() + interval

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            interval,
            pendingIntent
        )
    }
    private fun scheduleDailyWrapUpReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyWrapUpReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

}
class ProgressReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour < 10 || currentHour > 20) {
            return
        }
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val messages = mutableListOf(
            "Every step away from addiction is a step closer to freedom.",
            "The power to change lies within you.",
            "Small wins add up to big victories.",
            "You're stronger than you think.",
            "Every day without addiction is a triumph.",
            "Progress is progress, no matter how small.",
            "Focus on the future, not the past.",
            "Your strength today is your success tomorrow.",
            "One day at a time, you're winning.",
            "Believe in the person you're becoming.",
            "Keep pushing forward, you're doing great.",
            "The journey is tough, but so are you.",
            "Recovery is a journey, not a destination.",
            "Today is another chance to get it right.",
            "Your future is worth fighting for.",
            "Stay strong, your progress is showing.",
            "You’re proving your strength every day.",
            "Don't look back, you're not going that way.",
            "Celebrate every small victory.",
            "Log your progress in the app to keep the momentum going.",
            "Recovery is not a race, it's a journey. Take it one step at a time.",
            "Every challenge is an opportunity to grow stronger.",
            "You are capable of amazing things, believe in your potential.",
            "Small victories lead to big accomplishments. Keep going.",
            "Strength doesn’t come from what you can do, it comes from overcoming what you thought you couldn’t.",
            "Your future is created by what you do today, not tomorrow.",
            "The greatest battles we fight are the ones within ourselves.",
            "You’re stronger than you know, braver than you believe, and more capable than you can imagine.",
            "It’s never too late to be what you might have been.",
            "Don’t let the struggles of today ruin the strength of tomorrow.",
            "Focus on progress, not perfection. Every step forward matters.",
            "Courage doesn’t always roar; sometimes, it’s the quiet voice that says, ‘I’ll try again tomorrow.’",
            "Success is the sum of small efforts repeated day in and day out.",
            "One day at a time is all you need to focus on.",
            "The moment you want to quit is the moment you need to keep pushing.",
            "Recovery is hard, but regret is harder. Choose progress.",
            "Your story isn't over yet, you are still writing your success.",
            "Progress may be slow, but it’s still progress. Keep moving.",
            "You are not your past. You are the hero of your own future.",
            "Success starts with the decision to try. You're already winning.",
            "Healing is messy, but it’s a beautiful process. Trust the journey.",
            "A stumble does not mean you’ve fallen. Get back up stronger.",
            "Don't count the days, make the days count toward your freedom.",
            "You are enough just as you are, and more than capable of growth.",
            "Your hardest times often lead to the greatest moments of your life.",
            "In the middle of difficulty lies opportunity. Keep pushing forward.",
            "Each day without a relapse is a victory worth celebrating.",
            "Nothing changes if nothing changes. Take that first step today.",
            "The only way out is through. You’re closer to the other side than you think.",
            "Keep going, because you didn’t come this far just to come this far.",
            "The road to recovery is tough, but every mile makes you stronger.",
            "Your journey is unique, don’t compare it to anyone else’s story.",
            "Progress requires patience. Be kind to yourself on the journey.",
            "You’re doing better than you think. Celebrate the small wins.",
            "Every setback is a setup for a greater comeback.",
            "Remember why you started and let that drive you forward.",
            "Even the smallest steps in the right direction can be the biggest moments.",
            "You have the power to create the life you want. Keep fighting for it.",
            "The pain you feel today will be the strength you feel tomorrow.",
            "Rise up from your struggles; your strength grows with each challenge.",
            "Believe in the person you're becoming. You've got this.",
            "One day, all your hard work will pay off. Stay the course.",
            "You are not alone in this journey. Keep moving, keep growing.",
            "Challenges are what make life interesting; overcoming them is what makes life meaningful.",
            "You’ve survived 100% of your hardest days. Keep going.",
            "One positive thought in the morning can change your whole day.",
            "You are more resilient than you think. Trust your strength.",
            "Success doesn’t come from what you do occasionally; it comes from what you do consistently.",
            "Healing takes time. Don’t rush the process, trust it."
        )
        val randomMessage = messages.random()
        val notification = NotificationCompat.Builder(context, "progress_reminder")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("You got this!")
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}
class DailyWrapUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val addictionSet = sharedPreferences.getStringSet("addictions", null) ?: emptySet()

        val summary = addictionSet.joinToString(separator = "\n") { addiction ->
            val logStatusKey = "${addiction}_status"
            val logStatus = sharedPreferences.getString(logStatusKey, "No log") ?: "No log"
            val statusMatch = Regex(":\\s*\"?(.*?)\"?\\}").find(logStatus)
            val status = statusMatch?.groupValues?.get(1) ?: "No log"

            editor.remove(logStatusKey)

            "$addiction: $status"
        }


        editor.apply()


        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "progress_reminder")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Daily Wrap-Up")
            .setContentText("Here's how you did today!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification)
    }
}

