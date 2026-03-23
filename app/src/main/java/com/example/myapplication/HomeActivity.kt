package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private val PREFS_NAME = "RoutinePrefs"
    private var KEY_MORNING_DONE = "MorningDone_"
    private var KEY_EVENING_DONE = "EveningDone_"
    private var KEY_LAST_DATE = "LastDate"
    private var KEY_STREAK = "StreakCount"
    private var KEY_MORNING_ROUTINE = "MorningRoutine"
    private var KEY_EVENING_ROUTINE = "EveningRoutine"
    private var currentUserEmail: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Értesítések engedélyezve!", Toast.LENGTH_SHORT).show()
            // Ütemezzük újra őket, ha most kaptuk meg az engedélyt
            updateNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profilePrefs = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        val isLoggedIn = profilePrefs.getBoolean("IsLoggedIn", false)
        
        if (!isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()

        currentUserEmail = profilePrefs.getString("UserEmail", "") ?: ""

        KEY_MORNING_DONE = currentUserEmail + "_MorningDone_"
        KEY_EVENING_DONE = currentUserEmail + "_EveningDone_"
        KEY_LAST_DATE = currentUserEmail + "_LastDate"
        KEY_STREAK = currentUserEmail + "_StreakCount"
        KEY_MORNING_ROUTINE = currentUserEmail + "_MorningRoutine"
        KEY_EVENING_ROUTINE = currentUserEmail + "_EveningRoutine"

        val userName = intent.getStringExtra("USER_NAME") ?: profilePrefs.getString("UserName", "Szépségem") ?: "Szépségem"
        findViewById<TextView>(R.id.welcomeText).text = getString(R.string.home_welcome_format, userName)

        setDailyTip()
        checkAndResetStreak()
        updateStreakAndProgress()
        loadRoutineTitles()
        checkNotificationPermission()

        val mainView = findViewById<View>(R.id.main)
        val bottomNavCard = findViewById<View>(R.id.bottomNavCard)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            bottomNavCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 20 
            }
            insets
        }

        setupNavigation(userName)

        findViewById<View>(R.id.morningRoutineCard).setOnClickListener {
            startActivity(Intent(this, RoutineMorningActivity::class.java))
        }

        findViewById<View>(R.id.eveningRoutineCard).setOnClickListener {
            startActivity(Intent(this, RoutineEveningActivity::class.java))
        }

        findViewById<View>(R.id.skinDiaryCard).setOnClickListener {
            startActivity(Intent(this, SkinDiaryActivity::class.java))
        }

        findViewById<ImageView>(R.id.morningStatusIcon).setOnClickListener {
            toggleRoutine("morning", it)
        }

        findViewById<ImageView>(R.id.eveningStatusIcon).setOnClickListener {
            toggleRoutine("evening", it)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateNotifications() {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_UPDATE_NOTIFICATIONS
        }
        sendBroadcast(intent)
    }

    private fun setupNavigation(userName: String) {
        val navHome = findViewById<LinearLayout>(R.id.navHomeLayout)
        val navProducts = findViewById<LinearLayout>(R.id.navProductsLayout)
        val navStats = findViewById<LinearLayout>(R.id.navStatsLayout)
        val navProfile = findViewById<LinearLayout>(R.id.navProfileLayout)

        updateNavUI(navHome, true)

        navProducts.setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java).putExtra("USER_NAME", userName))
        }

        navStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java).putExtra("USER_NAME", userName))
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).putExtra("USER_NAME", userName))
        }
    }

    private fun updateNavUI(layout: LinearLayout, isActive: Boolean) {
        val icon = layout.getChildAt(0) as ImageView
        val text = layout.getChildAt(1) as TextView
        val color = if (isActive) R.color.deep_pink else R.color.slate_400
        
        icon.setColorFilter(ContextCompat.getColor(this, color))
        text.setTextColor(ContextCompat.getColor(this, color))
        text.paint.isFakeBoldText = isActive
    }

    private fun checkAndResetStreak() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        
        if (lastDate.isNotEmpty() && lastDate != today) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            
            if (lastDate != yesterday) {
                prefs.edit().putInt(KEY_STREAK, 0).apply()
            }
        }
    }

    private fun toggleRoutine(type: String, view: View) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val key = if (type == "morning") KEY_MORNING_DONE + today else KEY_EVENING_DONE + today
        
        val currentState = prefs.getBoolean(key, false)
        val morningDone = if (type == "morning") !currentState else prefs.getBoolean(KEY_MORNING_DONE + today, false)
        val eveningDone = if (type == "evening") !currentState else prefs.getBoolean(KEY_EVENING_DONE + today, false)
        
        val wasBothDone = prefs.getBoolean(KEY_MORNING_DONE + today, false) && prefs.getBoolean(KEY_EVENING_DONE + today, false)
        
        prefs.edit().putBoolean(key, !currentState).apply()
        
        val isBothDoneNow = morningDone && eveningDone
        
        if (!wasBothDone && isBothDoneNow) {
            handleStreakChange(true)
        } else if (wasBothDone && !isBothDoneNow) {
            handleStreakChange(false)
        }
        
        val anim = AnimationUtils.loadAnimation(this, R.anim.pop_in)
        view.startAnimation(anim)
        
        updateStreakAndProgress()
    }

    private fun handleStreakChange(increase: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val streak = prefs.getInt(KEY_STREAK, 0)
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""

        if (increase) {
            if (lastDate != today) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
                
                val newStreak = if (lastDate == yesterday || lastDate == "") streak + 1 else 1
                prefs.edit()
                    .putInt(KEY_STREAK, newStreak)
                    .putString(KEY_LAST_DATE, today)
                    .apply()
            }
        } else {
            if (lastDate == today) {
                val newStreak = if (streak > 0) streak - 1 else 0
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
                
                prefs.edit()
                    .putInt(KEY_STREAK, newStreak)
                    .putString(KEY_LAST_DATE, yesterday) 
                    .apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndResetStreak()
        updateStreakAndProgress()
        loadRoutineTitles()
    }

    private fun updateStreakAndProgress() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        
        val morningDone = prefs.getBoolean(KEY_MORNING_DONE + today, false)
        val eveningDone = prefs.getBoolean(KEY_EVENING_DONE + today, false)
        
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        if (morningDone && eveningDone && lastDate != today) {
            handleStreakChange(true)
        }

        var progress = 0
        if (morningDone) progress += 50
        if (eveningDone) progress += 50
        
        findViewById<LinearProgressIndicator>(R.id.streakProgress).progress = progress
        findViewById<TextView>(R.id.streakPercentage).text = getString(R.string.home_progress_format, progress)
        
        val morningIcon = findViewById<ImageView>(R.id.morningStatusIcon)
        val eveningIcon = findViewById<ImageView>(R.id.eveningStatusIcon)

        morningIcon.setImageResource(if (morningDone) R.drawable.bg_square_checked else R.drawable.bg_square_outline)
        eveningIcon.setImageResource(if (eveningDone) R.drawable.bg_square_checked else R.drawable.bg_square_outline)

        val streak = prefs.getInt(KEY_STREAK, 0)
        findViewById<TextView>(R.id.streakTitle).text = getString(R.string.home_streak_format, streak)
    }

    private fun loadRoutineTitles() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clickToCreate = getString(R.string.home_click_to_create)

        val morningRoutine = prefs.getString(KEY_MORNING_ROUTINE, null)
        val morningSummaryView = findViewById<TextView>(R.id.morningRoutineSummary)
        if (morningRoutine != null && morningRoutine != "[]") {
            try {
                val tasks = JSONArray(morningRoutine)
                val summary = mutableListOf<String>()
                for (i in 0 until minOf(tasks.length(), 3)) {
                    val taskName = tasks.getString(i).split("|")[0]
                    summary.add(taskName)
                }
                morningSummaryView?.text = if (summary.isEmpty()) clickToCreate else summary.joinToString(", ")
            } catch (e: Exception) {
                morningSummaryView?.text = clickToCreate
            }
        } else {
            morningSummaryView?.text = clickToCreate
        }

        val eveningRoutine = prefs.getString(KEY_EVENING_ROUTINE, null)
        val eveningSummaryView = findViewById<TextView>(R.id.eveningRoutineSummary)
        if (eveningRoutine != null && eveningRoutine != "[]") {
            try {
                val tasks = JSONArray(eveningRoutine)
                val summary = mutableListOf<String>()
                for (i in 0 until minOf(tasks.length(), 3)) {
                    val taskName = tasks.getString(i).split("|")[0]
                    summary.add(taskName)
                }
                eveningSummaryView?.text = if (summary.isEmpty()) clickToCreate else summary.joinToString(", ")
            } catch (e: Exception) {
                eveningSummaryView?.text = clickToCreate
            }
        } else {
            eveningSummaryView?.text = clickToCreate
        }
    }

    private fun setDailyTip() {
        val tips = resources.getStringArray(R.array.home_daily_tips)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        findViewById<TextView>(R.id.dailyTipText).text = tips[dayOfYear % tips.size]
    }
}
