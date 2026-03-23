package com.example.myapplication

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private val PREFS_NAME = "ProfilePrefs"
    private val KEY_SKIN_TYPE = "SkinType"
    private val KEY_MORNING_NOTIF = "MorningNotif"
    private val KEY_EVENING_NOTIF = "EveningNotif"
    private val KEY_MORNING_TIME = "MorningTime"
    private val KEY_EVENING_TIME = "EveningTime"
    private val KEY_USER_NAME = "UserName"
    private val KEY_USER_EMAIL = "UserEmail"
    private val KEY_PROFILE_IMAGE_URI = "ProfileImageUri"

    private var currentUserEmail: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Értesítések engedélyezve!", Toast.LENGTH_SHORT).show()
            checkExactAlarmPermission()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            savePreference(currentUserEmail + "_" + KEY_PROFILE_IMAGE_URI, it.toString())
            loadProfileImage(it.toString())
            Toast.makeText(this, "Profilkép frissítve!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean("IsLoggedIn", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        currentUserEmail = prefs.getString(KEY_USER_EMAIL, "") ?: ""

        setupSystemBars()
        updateProfileUI()
        setupSkinTypeButtons()
        setupNotifications()
        setupActionButtons(prefs)
        
        checkAndRequestPermissions()

        findViewById<ImageView>(R.id.profileImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val userName = prefs.getString(KEY_USER_NAME, "Anna") ?: "Anna"
        setupNavigation(userName)
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkExactAlarmPermission()
            }
        } else {
            checkExactAlarmPermission()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, "Kérlek engedélyezd a pontos riasztásokat a rutinokhoz!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            findViewById<View>(R.id.bottomNavCard).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 20
            }
            insets
        }
    }

    private fun setupNotifications() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val switchMorning = findViewById<SwitchMaterial>(R.id.switchMorning)
        val switchEvening = findViewById<SwitchMaterial>(R.id.switchEvening)
        val tvMorningTime = findViewById<TextView>(R.id.tvMorningTime)
        val tvEveningTime = findViewById<TextView>(R.id.tvEveningTime)

        switchMorning.isChecked = prefs.getBoolean(currentUserEmail + "_" + KEY_MORNING_NOTIF, true)
        switchEvening.isChecked = prefs.getBoolean(currentUserEmail + "_" + KEY_EVENING_NOTIF, true)
        tvMorningTime.text = prefs.getString(currentUserEmail + "_" + KEY_MORNING_TIME, "08:00")
        tvEveningTime.text = prefs.getString(currentUserEmail + "_" + KEY_EVENING_TIME, "20:00")

        tvMorningTime.setOnClickListener {
            showTimePicker(tvMorningTime.text.toString()) { time ->
                tvMorningTime.text = time
                savePreference(currentUserEmail + "_" + KEY_MORNING_TIME, time)
                updateAlarms()
            }
        }

        tvEveningTime.setOnClickListener {
            showTimePicker(tvEveningTime.text.toString()) { time ->
                tvEveningTime.text = time
                savePreference(currentUserEmail + "_" + KEY_EVENING_TIME, time)
                updateAlarms()
            }
        }

        switchMorning.setOnCheckedChangeListener { _, isChecked ->
            savePreference(currentUserEmail + "_" + KEY_MORNING_NOTIF, isChecked)
            updateAlarms()
        }

        switchEvening.setOnCheckedChangeListener { _, isChecked ->
            savePreference(currentUserEmail + "_" + KEY_EVENING_NOTIF, isChecked)
            updateAlarms()
        }
    }

    private fun updateAlarms() {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_UPDATE_NOTIFICATIONS
        }
        sendBroadcast(intent)
    }

    private fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = if (currentTime.contains(" ")) currentTime.split(" ")[1].split(":") else currentTime.split(":")
        TimePickerDialog(this, { _, h, m ->
            onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m))
        }, parts[0].toInt(), parts[1].toInt(), true).show()
    }

    private fun updateProfileUI() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        findViewById<TextView>(R.id.profileName).text = prefs.getString(KEY_USER_NAME, "Elena")
        findViewById<TextView>(R.id.profileEmail).text = currentUserEmail
        val uri = prefs.getString(currentUserEmail + "_" + KEY_PROFILE_IMAGE_URI, null)
        loadProfileImage(uri)
    }

    private fun loadProfileImage(uriString: String?) {
        val profileImage = findViewById<ImageView>(R.id.profileImage)
        if (uriString != null) {
            Glide.with(this).load(Uri.parse(uriString))
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                .circleCrop().into(profileImage)
            profileImage.setPadding(0, 0, 0, 0)
        } else {
            profileImage.setImageResource(R.drawable.ic_person)
            profileImage.setPadding(60, 60, 60, 60)
        }
    }

    private fun setupSkinTypeButtons() {
        val buttons = listOf(R.id.btnDry, R.id.btnOily, R.id.btnCombo, R.id.btnSensitive).map { findViewById<MaterialButton>(it) }
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val current = prefs.getString(currentUserEmail + "_" + KEY_SKIN_TYPE, "Zsíros")
        
        buttons.forEach { btn ->
            btn.setOnClickListener {
                updateSkinTypeUI(btn, buttons)
                savePreference(currentUserEmail + "_" + KEY_SKIN_TYPE, btn.text.toString())
            }
            if (btn.text.toString() == current) updateSkinTypeUI(btn, buttons)
        }
    }

    private fun updateSkinTypeUI(selected: MaterialButton, all: List<MaterialButton>) {
        all.forEach { btn ->
            val isSel = btn == selected
            btn.backgroundTintList = ColorStateList.valueOf(if (isSel) Color.parseColor("#F2C2C2") else Color.TRANSPARENT)
            btn.setTextColor(if (isSel) Color.WHITE else Color.parseColor("#64748B"))
            btn.strokeWidth = if (isSel) 0 else 2
        }
    }

    private fun setupActionButtons(prefs: android.content.SharedPreferences) {
        findViewById<View>(R.id.btnEditProfile).setOnClickListener { startActivity(Intent(this, EditProfileActivity::class.java)) }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            prefs.edit().putBoolean("IsLoggedIn", false).apply()
            val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
            startActivity(intent)
            finish()
        }
    }

    private fun setupNavigation(userName: String) {
        findViewById<LinearLayout>(R.id.navHomeLayout).setOnClickListener { startActivity(Intent(this, HomeActivity::class.java).putExtra("USER_NAME", userName)); finish() }
        findViewById<LinearLayout>(R.id.navProductsLayout).setOnClickListener { startActivity(Intent(this, ProductsActivity::class.java).putExtra("USER_NAME", userName)); finish() }
        findViewById<LinearLayout>(R.id.navStatsLayout).setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java).putExtra("USER_NAME", userName)); finish() }
        findViewById<ImageView>(R.id.ivNavProfile).setColorFilter(ContextCompat.getColor(this, R.color.deep_pink))
    }

    private fun savePreference(key: String, value: Any) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            if (value is String) putString(key, value) else if (value is Boolean) putBoolean(key, value)
            apply()
        }
    }
}
